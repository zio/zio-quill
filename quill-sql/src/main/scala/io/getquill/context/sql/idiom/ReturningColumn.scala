package io.getquill.context.sql.idiom

import io.getquill.NamingStrategy
import io.getquill.ast._

object ReturningColumn {
  def apply(ast: Ast)(implicit n: NamingStrategy): Option[String] = {
    val returningProperty = CollectAst(ast) {
      case Returning(_, property) => property
    }.headOption
    returningProperty.map(prop => returningColumn(ast, prop))
  }

  private def returningColumn(ast: Ast, prop: String)(implicit n: NamingStrategy) = {
    val entity = CollectAst(ast) {
      case Insert(e: Entity) => e
    }.head
    val propertyAlias = entity.properties.map(p => p.property -> p.alias).toMap
    propertyAlias.getOrElse(prop, n.column(prop))
  }
}
