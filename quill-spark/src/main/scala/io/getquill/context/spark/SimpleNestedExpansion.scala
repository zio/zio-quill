package io.getquill.context.spark

import io.getquill.ast._
import io.getquill.context.sql._
import io.getquill.quat.Quat
import io.getquill.sql.norm.StatelessQueryTransformer

object TopLevelExpansion {

  implicit class AliasOp(alias: Option[String]) {
    def concatWith(str: String): Option[String] =
      alias.orElse(Some("")).map(v => s"${v}${str}")
  }

  def apply(values: List[SelectValue], length: Int): List[SelectValue] =
    values.flatMap(apply(_, length))

  private def apply(value: SelectValue, length: Int): List[SelectValue] = {
    value match {
      case SelectValue(Tuple(values), alias, concat) =>
        values.zipWithIndex.map {
          case (ast, i) =>
            SelectValue(ast, Some(s"_${i + 1}"), concat)
        }
      case SelectValue(CaseClass(fields), alias, concat) =>
        fields.map {
          case (name, ast) =>
            SelectValue(ast, Some(name), concat)
        }
      case SelectValue(Ident(singleFieldName, Quat.Product(fields)), alias, concat) if (length == 1) =>
        fields.map {
          case (name, quat) =>
            SelectValue(Property(Ident(singleFieldName, quat), name), Some(name), concat)
        }.toList
      // Direct infix select, etc...
      case other if (length == 1) =>
        List(other.copy(alias = Some("single")))
      // Technically this case should not exist, adding it so that the pattern match will have full coverage
      case other =>
        List(other)
    }
  }
}

object SimpleNestedExpansion extends StatelessQueryTransformer {

  protected override def apply(q: SqlQuery, isTopLevel: Boolean = false): SqlQuery =
    q match {
      case q: FlattenSqlQuery => //if (isTopLevel) =>  //needs to be for all levels
        expandNested(q.copy(select = TopLevelExpansion(q.select, q.select.length))(q.quat), isTopLevel)
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
