package io.getquill.sql.norm

import io.getquill.NamingStrategy
import io.getquill.ast.Ast.LeafQuat
import io.getquill.ast.{Ident, Property, Renameable}
import io.getquill.context.sql._
import io.getquill.quat.Quat

object SheathIdentContexts extends StatelessQueryTransformer {

  override protected def expandNested(q: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery = {
    val from = q.from.map(expandContext(_))
    val select = q.select.map { selectValue =>
      selectValue.ast match {
        case LeafQuat(origId: Ident) =>
          val selects = findSelectsOfAlias(from, selectValue.alias.getOrElse(origId.name))
          selects.map(_.alias).distinct match {
            // if it's a single value of a single subselect eg:
            // SELECT z FROM (SELECT x.i FROM foo) AS z)
            // or
            // SELECT z FROM (SELECT x.i FROM foo) UNION (SELECT y.i FROM bar) AS z)
            case List(Some(value)) =>
              // Then make it into:
              // SELECT z.i FROM (SELECT x.i FROM foo) AS z.i)
              // or
              // SELECT z.i FROM (SELECT x.i FROM foo) UNION (SELECT y.i FROM bar) AS z.i)
              // this z.i will be:
              // Property(Ident(z, CC(i -> zOrig.quat), "i")
              val newId = Ident(origId.name, Quat.Product("<single-prop-gen>", value -> origId.quat))
              SelectValue(Property(newId, value))
            case _ =>
              selectValue
          }

        case _ => selectValue
      }
    }
    q.copy(select = select, from = from)(q.quat)
  }

  // find selects of a query aliased as X
  protected def findSelectsOfAlias(contexts: List[FromContext], alias: String): List[SelectValue] = {
    def handleQuery(q: SqlQuery, queryAlias: String): List[SelectValue] =
      q match {
        case SetOperationSqlQuery(a, _, b) => handleQuery(a, queryAlias) ++ handleQuery(b, queryAlias)
        case UnaryOperationSqlQuery(_, q)  => handleQuery(q, queryAlias)
        case flatten: FlattenSqlQuery      => flatten.select
      }

    contexts.flatMap {
      case QueryContext(q, queryAlias) =>
        if (queryAlias == alias)
          handleQuery(q, queryAlias)
        else
          List()

      case JoinContext(_, a, b, _) =>
        findSelectsOfAlias(List(a), alias) ++ findSelectsOfAlias(List(b), alias)

      case FlatJoinContext(_, a, _) =>
        findSelectsOfAlias(List(a), alias)

      // table or infix would be a single variable, not sure can't do anything in that case
      case _: TableContext | _: InfixContext => List()
    }
  }
}
