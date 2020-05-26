package io.getquill.dsl

import io.getquill.util.MacroContextExt._
import scala.reflect.macros.whitebox.{ Context => MacroContext }

class MetaDslMacro(val c: MacroContext) extends ValueComputation {
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
              ${c.prefix}.quote(${c.prefix}.impliedQuerySchema[$t](${t.tpe.typeSymbol.name.decodedName.toString}))
            def entity = _entity
          }
        """
    } else {
      c.fail(s"Can't materialize a `SchemaMeta` for non-case-class type '${t.tpe}', please provide an implicit `SchemaMeta`.")
    }

  private def expandQuery[T](value: Value)(implicit t: WeakTypeTag[T]) = {
    val elements = flatten(q"x", value)
    if (elements.isEmpty)
      c.fail(s"Case class type ${t.tpe} has no values")
    q"${c.prefix}.quote((q: io.getquill.Query[$t]) => q.map(x => io.getquill.dsl.UnlimitedTuple(..$elements)))"
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
            val groups = params.map(_.map(v => expand(v, parentOptional = true) -> v.nestedAndOptional))
            val terms =
              groups.zipWithIndex.map {
                case (options, idx1) =>
                  options.zipWithIndex.map {
                    case ((_, opt), idx2) =>
                      val term = TermName(s"o_${idx1}_$idx2")
                      if (opt) q"Some($term)"
                      else q"$term"
                  }
              }
            groups.zipWithIndex.foldLeft(q"Some(new $tpe(...$terms))") {
              case (body, (options, idx1)) =>
                options.zipWithIndex.foldLeft(body) {
                  case (body, ((option, _), idx2)) =>
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
            ${c.prefix}.quote((q: io.getquill.EntityQuery[$t], value: $t) => q.${TermName(method)}(..$assignments))
          def expand = _expand
        }
      """
    }
  }
}