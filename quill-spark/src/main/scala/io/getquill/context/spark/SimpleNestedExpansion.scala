package io.getquill.context.spark

import io.getquill.ast._
import io.getquill.context.sql._
import io.getquill.sql.norm.StatelessQueryTransformer

object TopLevelExpansion {

  implicit class AliasOp(alias: Option[String]) {
    def concatWith(str: String): Option[String] =
      alias.orElse(Some("")).map(v => s"${v}${str}")
  }

  def apply(values: List[SelectValue]): List[SelectValue] =
    values.flatMap(apply(_))

  private def apply(value: SelectValue): List[SelectValue] = {
    value match {
      case SelectValue(Tuple(values), alias, concat) =>
        values.zipWithIndex.map {
          case (ast, i) =>
            SelectValue(ast, alias.concatWith(s"_${i + 1}"), concat)
        }
      //      case SelectValue(CaseClass(fields), alias, concat) =>
      //        fields.flatMap {
      //          case (name, ast) =>
      //            apply(SelectValue(ast, alias.concatWith(name), concat))
      //        }
      // Direct infix select, etc...
      case other => List(other)
    }
  }
}

object SimpleNestedExpansion extends StatelessQueryTransformer {

  protected override def apply(q: SqlQuery, isTopLevel: Boolean = false): SqlQuery =
    q match {
      case q: FlattenSqlQuery =>
        expandNested(q.copy(select = TopLevelExpansion(q.select))(q.quat), isTopLevel)
      case other =>
        super.apply(q, isTopLevel)
    }

  protected override def expandNested(q: FlattenSqlQuery, isTopLevel: Boolean): FlattenSqlQuery =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
        val newFroms = q.from.map(expandContext(_))

        def distinctIfNotTopLevel(values: List[SelectValue]) =
          if (isTopLevel)
            values
          else
            values.distinct

        val distinctSelects =
          distinctIfNotTopLevel(select)

        q.copy(select = distinctSelects, from = newFroms)(q.quat)
    }
}
