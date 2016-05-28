package io.getquill.quotation

import io.getquill.ast.PropertyAlias

import scala.reflect.macros.whitebox.Context

case class EntityConfig(
  alias:      Option[String]      = None,
  properties: List[PropertyAlias] = List(),
  generated:  Option[String]      = None
)

trait EntityConfigParsing extends UnicodeArrowParsing {
  this: Parsing =>
  val c: Context

  import c.universe.{ Function => _, Ident => _, _ }

  def parseEntityConfig(t: Tree): EntityConfig =
    t match {
      case q"$e.entity(${ name: String })" =>
        parseEntityConfig(e).copy(alias = Some(name))
      case q"$e.columns(..$propertyAliases)" =>
        parseEntityConfig(e).copy(properties = propertyAliases.map(propertyAliasParser(_)))
      case q"$e.generated(($alias) => $body)" =>
        parseEntityConfig(e).copy(generated = Some(parseProperty(body)))
      case _ =>
        EntityConfig()
    }

  private def parseProperty(t: Tree): String =
    t match {
      case q"$e.$property" => property.decodedName.toString
    }

  val propertyAliasParser: Parser[PropertyAlias]
}
