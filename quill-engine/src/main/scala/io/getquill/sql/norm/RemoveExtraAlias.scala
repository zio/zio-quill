package io.getquill.sql.norm

import io.getquill.NamingStrategy
import io.getquill.ast.{ Property, Renameable }
import io.getquill.context.sql.{ FlattenSqlQuery, SelectValue }
import io.getquill.sql.norm.RemoveExtraAlias.TopLevelRemove

object RemoveExtraAlias {
  sealed trait TopLevelRemove
  case object TopLevelRemove {
    /**
     * Remove all top-level aliases. This is for top-level queries where aliases are not needed at all
     * since Quill extractors use the position value instead
     */
    case object All extends TopLevelRemove
    /** Remove only aliases on the top level that match the column name (same as rest of the query) */
    case object OnlyMatching extends TopLevelRemove
  }
}

/**
 * Remove aliases at the top level of the AST since they are not needed
 * (quill uses select row indexes to figure out what data corresponds to what encodeable object)
 * as well as entities whose aliases are the same as their selection e.g. "select x.foo as foo"
 * since this just adds syntactic noise.
 */
case class RemoveExtraAlias(strategy: NamingStrategy, topLevel: TopLevelRemove = TopLevelRemove.All) extends StatelessQueryTransformer {
  // Remove aliases that are the same as as the select values. Since a strategy may change the name,
  // use a heuristic where if the column naming strategy make the property name be different from the
  // alias, keep the column property name.
  // Note that in many cases e.g. tuple names _1,_2 etc... the column name will be rarely changed,
  // as a result of column capitalization, however it is possible that it will be changed as a result
  // of some other scheme (e.g. adding 'col' to every name where columns actually named _1,_2 become col_1,col_2)
  // and then unless the proper alias is there (e.g. foo.col_1 AS _1, foo.col_2 AS _2) subsequent selects will incorrectly
  // select _1.foo,_2.bar fields assuming the _1,_2 columns actually exist.
  // However, in situations where the actual column name is Fixed, these kinds of things typically
  // will not happen so we do not force the alias to happen.
  // Note that in certain situations this will happen anyway (e.g. if the user overwrites the tokenizeFixedColumn method
  // in SqlIdiom. In those kinds of situations we allow specifying the -Dquill.query.alwaysAlias
  def strategyMayChangeColumnName(p: Property): Boolean =
    p.renameable == Renameable.ByStrategy && strategy.column(p.name) != p.name

  private def removeUnneededAlias(value: SelectValue): SelectValue =
    value match {
      // Can remove an alias if the alias is equal to the property name and the property name cannot be changed but the naming strategy
      case sv @ SelectValue(p: Property, Some(alias), _) if !strategyMayChangeColumnName(p) && p.name == alias =>
        sv.copy(alias = None)
      case _ =>
        value
    }

  override protected def expandNested(q: FlattenSqlQuery, isTopLevel: Boolean): FlattenSqlQuery = {
    val from = q.from.map(expandContext(_))
    val select =
      q.select
        .map(removeUnneededAlias(_))
        .map { sv =>
          // If we are on the top-level query and instructed to remove all the aliases inside then do that
          if (isTopLevel && topLevel == TopLevelRemove.All)
            sv.copy(alias = None)
          else
            sv
        }
    q.copy(select = select, from = from)(q.quat)
  }
}
