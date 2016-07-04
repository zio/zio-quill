package io.getquill.context.sql

import io.getquill.ast.Ast
import io.getquill.ast.Entity
import io.getquill.ast.Ident
import io.getquill.ast.Query
import io.getquill.context.ExtractEntityAndInsertAction
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.norm.ExpandJoin
import io.getquill.context.sql.norm.ExpandNestedQueries
import io.getquill.context.sql.norm.MergeSecondaryJoin
import io.getquill.NamingStrategy
import io.getquill.norm.FlattenOptionOperation
import io.getquill.norm.Normalize
import io.getquill.norm.RenameAssignments
import io.getquill.norm.RenameProperties
import io.getquill.util.Messages.fail
import io.getquill.util.Show.Shower

object Prepare {

  def apply(ast: Ast, params: List[Ident])(implicit d: SqlIdiom, n: NamingStrategy) = {
    val (bindedAst, idents) = BindVariables(normalize(ast), params)
    import d._
    val sqlString =
      bindedAst match {
        case q: Query =>
          val sql = SqlQuery(q)
          VerifySqlQuery(sql).map(fail)
          ExpandNestedQueries(sql, collection.Set.empty).show
        case other =>
          other.show
      }

    val (entity, insert) = ExtractEntityAndInsertAction(bindedAst)
    val isInsert = insert.isDefined

    val generated = if (isInsert) entity.flatMap(nameGeneratedColumn) else None

    (sqlString, idents, generated)
  }

  private def nameGeneratedColumn(entity: Entity)(implicit n: NamingStrategy): Option[String] = {
    val propertyAlias = entity.properties.map(p => p.property -> p.alias).toMap
    entity.generated.map(g => propertyAlias.getOrElse(g, n.column(g)))
  }

  private[this] val normalize =
    (identity[Ast] _)
      .andThen(Normalize.apply _)
      .andThen(ExpandJoin.apply _)
      .andThen(Normalize.apply _)
      .andThen(MergeSecondaryJoin.apply _)
      .andThen(FlattenOptionOperation.apply _)
      .andThen(RenameAssignments.apply _)
      .andThen(RenameProperties.apply _)

}
