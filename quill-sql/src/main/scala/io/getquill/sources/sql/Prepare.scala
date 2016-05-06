package io.getquill.sources.sql

import io.getquill.ast._
import io.getquill.norm.{ RenameProperties, RenameAssignments, Normalize, FlattenOptionOperation }
import io.getquill.naming.NamingStrategy
import io.getquill.sources.ExtractEntityAndInsertAction
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.util.Show._
import io.getquill.util.Messages._
import io.getquill.sources.sql.norm.ExpandJoin
import io.getquill.sources.sql.norm.ExpandNestedQueries
import io.getquill.sources.sql.norm.MergeSecondaryJoin

object Prepare {

  def apply(ast: Ast, params: List[Ident])(implicit d: SqlIdiom, n: NamingStrategy) = {
    import d._
    val (bindedAst, idents) = BindVariables(normalize(ast), params)
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
