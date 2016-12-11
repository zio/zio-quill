package io.getquill.dsl

import io.getquill.Embedded
import io.getquill.util.Messages._
import io.getquill.util.OptionalTypecheck

import scala.reflect.macros.whitebox.{ Context => MacroContext }

class MetaDslMacro(val c: MacroContext) {

  import c.universe._

  def schemaMeta[T](entity: Tree, columns: Tree*)(implicit t: WeakTypeTag[T]): Tree =
    c.untypecheck {
      q"""
        new ${c.prefix}.SchemaMeta[$t] {
          val entity =
            ${c.prefix}.quote {
              ${c.prefix}.querySchema[$t]($entity, ..$columns)
            }
        }
      """
    }

  def queryMeta[T, R](expand: Tree)(extract: Tree)(implicit t: WeakTypeTag[T], r: WeakTypeTag[R]): Tree =
    c.untypecheck {
      q"""
        new ${c.prefix}.QueryMeta[$t] {
          val expand = $expand
          val extract =
            (r: ${c.prefix}.ResultRow) => $extract(implicitly[${c.prefix}.QueryMeta[$r]].extract(r))
        }
      """
    }

  def insertMeta[T](exclude: Tree*)(implicit t: WeakTypeTag[T]): Tree =
    actionMeta[T](value("Encoder", t.tpe, exclude: _*), "insert")

  def updateMeta[T](exclude: Tree*)(implicit t: WeakTypeTag[T]): Tree =
    actionMeta[T](value("Encoder", t.tpe, exclude: _*), "update")

  def materializeQueryMeta[T](implicit t: WeakTypeTag[T]): Tree = {
    val value = this.value("Decoder", t.tpe)
    q"""
      new ${c.prefix}.QueryMeta[$t] {
        val expand = ${expandQuery[T](value)}
        val extract = ${extract[T](value)}
      }
    """
  }

  def materializeUpdateMeta[T](implicit t: WeakTypeTag[T]): Tree =
    actionMeta[T](value("Encoder", t.tpe), "update")

  def materializeInsertMeta[T](implicit t: WeakTypeTag[T]): Tree =
    actionMeta[T](value("Encoder", t.tpe), "insert")

  def materializeSchemaMeta[T](implicit t: WeakTypeTag[T]): Tree =
    if (t.tpe.typeSymbol.isClass && t.tpe.typeSymbol.asClass.isCaseClass) {
      q"""
          new ${c.prefix}.SchemaMeta[$t] {
            val entity =
              ${c.prefix}.quote(${c.prefix}.querySchema[$t](${t.tpe.typeSymbol.name.decodedName.toString}))
          }
        """
    } else {
      c.fail(s"Can't materialize a `SchemaMeta` for non-case-class type '${t.tpe}', please provide an implicit `SchemaMeta`.")
    }

  private def expandQuery[T](value: Value)(implicit t: WeakTypeTag[T]) = {
    val elements = flatten(q"x", value)
    q"${c.prefix}.quote((q: ${c.prefix}.Query[$t]) => q.map(x => io.getquill.dsl.UnlimitedTuple(..$elements)))"
  }

  private def extract[T](value: Value)(implicit t: WeakTypeTag[T]): Tree = {
    var index = -1

    def expand(value: Value, optional: Boolean = false): Tree =
      value match {

        case Scalar(path, tpe, decoder, _) =>
          index += 1
          if (optional) {
            q"implicitly[${c.prefix}.Decoder[Option[$tpe]]].apply($index, row)"
          } else {
            q"$decoder($index, row)"
          }

        case Nested(term, tpe, params, _) =>
          q"new $tpe(...${params.map(_.map(expand(_)))})"

        case OptionalNested(term, tpe, params, _) =>
          val groups = params.map(_.map(expand(_, optional = true)))
          val terms =
            groups.zipWithIndex.map {
              case (options, idx1) =>
                options.indices.map { idx2 =>
                  TermName(s"o_${idx1}_$idx2")
                }
            }
          groups.zipWithIndex.foldLeft(q"Some(new $tpe(...$terms))") {
            case (body, (options, idx1)) =>
              options.zipWithIndex.foldLeft(body) {
                case (body, (option, idx2)) =>
                  val o = q"val ${TermName(s"o_${idx1}_$idx2")} = $EmptyTree"
                  q"$option.flatMap($o => $body)"
              }
          }
      }

    q"(row: ${c.prefix}.ResultRow) => ${expand(value)}"
  }

  private def actionMeta[T](value: Value, method: String)(implicit t: WeakTypeTag[T]) = {
    val assignments =
      flatten(q"v", value)
        .zip(flatten(q"value", value))
        .map {
          case (vTree, valueTree) =>
            q"(v: $t) => $vTree -> $valueTree"
        }
    c.untypecheck {
      q"""
        new ${c.prefix}.${TypeName(method.capitalize + "Meta")}[$t] {
          val expand =
            ${c.prefix}.quote((q: ${c.prefix}.EntityQuery[$t], value: $t) => q.${TermName(method)}(..$assignments))
        }
      """
    }
  }

  def flatten(base: Tree, value: Value): List[Tree] = {
    def nest(tree: Tree, term: TermName) =
      q"$tree.$term"

    def nestOpt(tree: Tree, term: Option[TermName]) =
      term match {
        case None    => tree
        case Some(t) => nest(tree, t)
      }

    def apply(base: Tree, params: List[List[Value]]): List[Tree] =
      params.flatten.flatMap(flatten(base, _))

    def wrapNotPublic(term: TermName, tpe: Type, position: Option[Int]): Tree = {
      val get = s"get${position.map(p => s"._${p + 1}").getOrElse("")}"
      val d = NotPublicWrapper.prefix + term.decodedName.toString
      c.parse(s"new ${typeOf[NotPublicWrapper]} { def $d = $base; def $term = $tpe.unapply($d).$get }")
    }

    value match {
      case Scalar(termOption, _, _, accessLevel) =>
        (termOption, accessLevel) match {
          case (Some(term), Other(parentTpe, position)) =>
            List(nest(wrapNotPublic(term, parentTpe, position), term))
          case _ => List(nestOpt(base, termOption))
        }
      case Nested(termOption, _, params, accessLevel) =>
        val nested =
          (termOption, accessLevel) match {
            case (Some(term), Other(parentTpe, position)) =>
              nest(wrapNotPublic(term, parentTpe, position), term)
            case _ =>
              nestOpt(base, termOption)
          }
        apply(nested, params)
      case OptionalNested(termOption, _, params, accessLevel) =>
        val nested =
          (termOption, accessLevel) match {
            case (Some(term), Other(parentTpe, position)) =>
              nest(wrapNotPublic(term, parentTpe, position), term)
            case _ =>
              nestOpt(base, termOption)
          }

        apply(q"v", params)
          .map(body => q"$nested.map(v => $body)")
    }
  }

  sealed trait Value {
    val term: Option[TermName]
  }

  case class Nested(term: Option[TermName], tpe: Type, params: List[List[Value]], accessLevel: ValueAccessLevel) extends Value

  case class OptionalNested(term: Option[TermName], tpe: Type, params: List[List[Value]], accessLevel: ValueAccessLevel) extends Value

  case class Scalar(term: Option[TermName], tpe: Type, decoder: Tree, accessLevel: ValueAccessLevel) extends Value

  sealed trait ValueAccessLevel

  object Public extends ValueAccessLevel

  case class Other(parent: Type, position: Option[Int]) extends ValueAccessLevel

  private def is[T](tpe: Type)(implicit t: TypeTag[T]) =
    tpe <:< t.tpe

  private def constructorParamHasPublicAccessor(tpe: Type, param: Symbol): Boolean =
    tpe.members.collect {
      case m: MethodSymbol if m.isPublic && m.isGetter && m.name == param.name.toTermName => m
    }.nonEmpty

  private def value(encoding: String, tpe: Type, exclude: Tree*): Value = {

    def nest(tpe: Type, term: Option[TermName], accessLevel: ValueAccessLevel): Nested =
      caseClassConstructor(tpe) match {
        case None =>
          c.fail(s"Found the embedded '$tpe', but it is not a case class")
        case Some(constructor) =>
          val params =
            constructor.paramLists.map { params =>
              params.zipWithIndex.map {
                case (param, index) =>
                  apply(
                    param.typeSignature.asSeenFrom(tpe, tpe.typeSymbol),
                    Some(param.name.toTermName),
                    nested = !isTuple(tpe),
                    if (constructorParamHasPublicAccessor(tpe, param)) Public
                    else Other(tpe, if (params.size > 1) Some(index) else None)
                  )
              }
            }
          Nested(term, tpe, params, accessLevel)
      }

    def apply(tpe: Type, term: Option[TermName], nested: Boolean, accessLevel: ValueAccessLevel): Value = {
      OptionalTypecheck(c)(q"implicitly[${c.prefix}.${TypeName(encoding)}[$tpe]]") match {
        case Some(encoding) => Scalar(term, tpe, encoding, accessLevel)
        case None =>

          def value(tpe: Type) =
            tpe match {
              case tpe if !is[Embedded](tpe) && nested =>
                c.fail(
                  s"Can't expand nested value '$tpe', please make it an `Embedded` " +
                    s"case class or provide an implicit $encoding for it."
                )

              case tpe =>
                nest(tpe, term, accessLevel)
            }

          if (is[Option[Any]](tpe)) {
            val nested = value(tpe.typeArgs.head)
            OptionalNested(nested.term, nested.tpe, nested.params, nested.accessLevel)
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
                case other                   => Nil
              }

            path(f.body)
        }

      def filter(value: Value, path: List[TermName] = Nil): Option[Value] =
        value match {
          case value if paths.contains(path ++ value.term) =>
            None
          case Nested(term, tpe, params, accessLevel) =>
            Some(Nested(term, tpe, params.map(_.flatMap(filter(_, path ++ term))), accessLevel))
          case OptionalNested(term, tpe, params, accessLevel) =>
            Some(OptionalNested(term, tpe, params.map(_.flatMap(filter(_, path ++ term))), accessLevel))
          case value =>
            Some(value)
        }

      filter(value).getOrElse {
        c.fail("Can't exclude all entity properties")
      }
    }

    filterExcludes(apply(tpe, term = None, nested = false, Public))
  }

  private def isTuple(tpe: Type) =
    tpe.typeSymbol.name.toString.startsWith("Tuple")

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.headOption
}