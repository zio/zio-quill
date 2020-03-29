package io.getquill.dsl

import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.Embedded
import io.getquill.util.OptionalTypecheck
import io.getquill.util.MacroContextExt._

trait ValueComputation {
  val c: MacroContext
  import c.universe._

  sealed trait Value {
    val term: Option[TermName]
    def nestedAndOptional: Boolean
  }
  case class Nested(term: Option[TermName], tpe: Type, params: List[List[Value]], optional: Boolean) extends Value {
    def nestedAndOptional: Boolean = optional
  }
  case class Scalar(term: Option[TermName], tpe: Type, decoder: Tree, optional: Boolean) extends Value {
    def nestedAndOptional: Boolean = false
  }

  private def is[T](tpe: Type)(implicit t: TypeTag[T]) =
    tpe <:< t.tpe

  private[getquill] def value(encoding: String, tpe: Type, exclude: Tree*): Value = {

    def nest(tpe: Type, term: Option[TermName]): Nested =
      caseClassConstructor(tpe) match {
        case None =>
          c.fail(s"Found the embedded '$tpe', but it is not a case class")
        case Some(constructor) =>
          val params =
            constructor.paramLists.map {
              _.map { param =>
                apply(
                  param.typeSignature.asSeenFrom(tpe, tpe.typeSymbol),
                  Some(param.name.toTermName),
                  nested = !isTuple(tpe)
                )
              }
            }
          Nested(term, tpe, params, optional = false)
      }

    def apply(tpe: Type, term: Option[TermName], nested: Boolean): Value = {
      OptionalTypecheck(c)(q"implicitly[${c.prefix}.${TypeName(encoding)}[$tpe]]") match {
        case Some(encoding) =>
          Scalar(term, tpe, encoding, optional = is[Option[Any]](tpe))
        case None =>

          def value(tpe: Type) =
            tpe match {
              case tpe if !is[Embedded](tpe) && nested =>
                c.fail(
                  s"""Can't find implicit `$encoding[$tpe]`. Please, do one of the following things:
                     |1. ensure that implicit `$encoding[$tpe]` is provided and there are no other conflicting implicits;
                     |2. make `$tpe` `Embedded` case class or `AnyVal`.
                   """.stripMargin
                )

              case tpe =>
                nest(tpe, term)
            }

          if (isNone(tpe)) {
            c.fail("Cannot handle untyped `None` objects. Use a cast e.g. `None:Option[String]` or `Option.empty`.")
          } else if (is[Option[Any]](tpe)) {
            value(tpe.typeArgs.head).copy(optional = true)
          } else {
            value(tpe)
          }
      }
    }

    def filterExcludes(value: Value) = {
      val paths =
        exclude.map {
          case f: Function =>
            def path(tree: Tree): List[TermName] =
              tree match {
                case q"$a.$b"                => path(a) :+ b
                case q"$a.map[$t]($b => $c)" => path(a) ++ path(c)
                case _                       => Nil
              }
            path(f.body)
        }

      def filter(value: Value, path: List[TermName] = Nil): Option[Value] =
        value match {
          case value if paths.contains(path ++ value.term) =>
            None
          case Nested(term, tpe, params, optional) =>
            Some(Nested(term, tpe, params.map(_.flatMap(filter(_, path ++ term))), optional))
          case value =>
            Some(value)
        }

      filter(value).getOrElse {
        c.fail("Can't exclude all entity properties")
      }
    }

    filterExcludes(apply(tpe, term = None, nested = false))
  }

  private def isNone(tpe: Type) =
    tpe.typeSymbol.name.toString == "None"

  private def isTuple(tpe: Type) =
    tpe.typeSymbol.name.toString.startsWith("Tuple")

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.headOption

  def flatten(base: Tree, value: Value): List[Tree] = {
    def nest(tree: Tree, term: Option[TermName]) =
      term match {
        case None       => tree
        case Some(term) => q"$tree.$term"
      }

    def apply(base: Tree, params: List[List[Value]]): List[Tree] =
      params.flatten.flatMap(flatten(base, _))

    value match {
      case Scalar(term, _, _, _) =>
        List(nest(base, term))
      case Nested(term, _, params, optional) =>
        if (optional)
          apply(q"v", params)
            .map(body => q"${nest(base, term)}.map(v => $body)")
        else
          apply(nest(base, term), params)
    }
  }
}
