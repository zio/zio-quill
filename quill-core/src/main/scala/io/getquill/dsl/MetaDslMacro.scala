package io.getquill.dsl

import io.getquill.Embedded
import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.util.OptionalTypecheck

class MetaDslMacro(val c: MacroContext) {
  import c.universe._

  def schemaMeta[T](entity: Tree, columns: Tree*)(implicit t: WeakTypeTag[T]): Tree =
    c.untypecheck {
      q"""
        new ${c.prefix}.SchemaMeta[$t] {
          private[this] val _entity = ${c.prefix}.quote {
            ${c.prefix}.querySchema[$t]($entity, ..$columns)
          }
          def entity = _entity
        }
      """
    }

  def queryMeta[T, R](expand: Tree)(extract: Tree)(implicit t: WeakTypeTag[T], r: WeakTypeTag[R]): Tree =
    c.untypecheck {
      q"""
        new ${c.prefix}.QueryMeta[$t] {
          private[this] val _expand = $expand
          def expand = _expand
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
        private[this] val _expanded = ${expandQuery[T](value)}
        def expand = _expanded
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
            private[this] val _entity =
              ${c.prefix}.quote(${c.prefix}.querySchema[$t](${t.tpe.typeSymbol.name.decodedName.toString}))
            def entity = _entity
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

    def expand(value: Value, parentOptional: Boolean = false): Tree =
      value match {

        case Scalar(_, tpe, decoder, optional) =>
          index += 1
          if (parentOptional) {
            if (optional)
              q"Some($decoder($index, row))"
            else
              q"implicitly[${c.prefix}.Decoder[Option[$tpe]]].apply($index, row)"
          } else {
            q"$decoder($index, row)"
          }

        case Nested(_, tpe, params, optional) =>
          if (parentOptional || optional) {
            val groups = params.map(_.map(expand(_, parentOptional = true)))
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
          } else
            q"new $tpe(...${params.map(_.map(expand(_)))})"
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
          private[this] val _expand =
            ${c.prefix}.quote((q: ${c.prefix}.EntityQuery[$t], value: $t) => q.${TermName(method)}(..$assignments))
          def expand = _expand
        }
      """
    }
  }

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

  sealed trait Value {
    val term: Option[TermName]
  }
  case class Nested(term: Option[TermName], tpe: Type, params: List[List[Value]], optional: Boolean) extends Value
  case class Scalar(term: Option[TermName], tpe: Type, decoder: Tree, optional: Boolean) extends Value

  private def is[T](tpe: Type)(implicit t: TypeTag[T]) =
    tpe <:< t.tpe

  private def value(encoding: String, tpe: Type, exclude: Tree*): Value = {

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
                  s"Can't expand nested value '$tpe', please make it an `Embedded` " +
                    s"case class or provide an implicit $encoding for it."
                )

              case tpe =>
                nest(tpe, term)
            }

          if (is[Option[Any]](tpe)) {
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

  private def isTuple(tpe: Type) =
    tpe.typeSymbol.name.toString.startsWith("Tuple")

  private def caseClassConstructor(t: Type) =
    t.members.collect {
      case m: MethodSymbol if m.isPrimaryConstructor => m
    }.headOption
}