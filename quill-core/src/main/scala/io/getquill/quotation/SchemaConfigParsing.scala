package io.getquill.quotation

import io.getquill.ast.PropertyAlias

import scala.reflect.macros.whitebox.Context

case class SchemaConfig(alias: Option[String] = None, properties: List[PropertyAlias] = List(), generated: Option[String] = None)

trait SchemaConfigParsing extends UnicodeArrowParsing {
  val c: Context

  import c.universe.{ Function => _, Ident => _, _ }

  def parseEntityConfig(t: Tree): SchemaConfig =
    t match {
      case q"$e.entity(${ name: String })" =>
        parseEntityConfig(e).copy(alias = Some(name))
      case q"$e.columns(..$propertyAliases)" =>
        parseEntityConfig(e).copy(properties = propertyAliases.map(parsePropertyAlias))
      case q"$e.generated(($alias) => $body)" =>
        parseEntityConfig(e).copy(generated = Some(parseProperty(body)))
      case _ =>
        SchemaConfig()
    }

  private def parseProperty(t: Tree): String =
    t match {
      case q"$e.$property" => property.decodedName.toString
    }

  private def parsePropertyAlias(t: Tree): PropertyAlias =
    t match {
      case q"(($x1) => scala.this.Predef.ArrowAssoc[$t]($x2.$prop).$arrow[$v](${ alias: String }))" =>
        PropertyAlias(prop.decodedName.toString, alias)
    }
}
