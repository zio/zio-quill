package io.getquill.sql.norm

import io.getquill.NamingStrategy
import io.getquill.ast.Ast.LeafQuat
import io.getquill.ast.{Ast, CollectAst, Ident, Property, Renameable}
import io.getquill.context.sql.{FlatJoinContext, FlattenSqlQuery, FromContext, InfixContext, JoinContext, QueryContext, SelectValue, TableContext}
import io.getquill.norm.{BetaReduction, TypeBehavior}
import io.getquill.quat.Quat

// If we run this right after SqlQuery we know that in every place with a single select-value it is a leaf clause e.g. `SELECT x FROM (SELECT p.name from Person p)) AS x`
// in that case we know that SelectValue(x) is a leaf clause that we should expand into a `x.value`.
// MAKE SURE THIS RUNS BEFORE ExpandNestedQueries otherwise it will be incorrect, it should only run for single-selects from atomic values,
// if the ExpandNestedQueries ran it could be a single field that is coming from a case class e.g. case class MySingleValue(stuff: Int) that is being selected from
case class ValueizeSingleLeafSelects(strategy: NamingStrategy) extends StatelessQueryTransformer {
  protected def productize(ast: Ident) =
    Ident(ast.name, Quat.Product("<Value>", "value" -> Quat.Value))

  protected def valueize(ast: Ident) =
    Property(productize(ast), "value")

  // Turn every `SELECT primitive-x` into a `SELECT case-class-x.primitive-value`
  override protected def expandNested(q: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery = {
    // get the alises before we transform (i.e. Valueize) the contexts inside turning the leaf-quat alises into product-quat alises
    val leafValuedFroms = collectAliases(q.from).filter(!_.quat.isProduct)
    // now transform the inner clauses
    val from = q.from.map(expandContext(_))

    def containsAlias(ast: Ast): Boolean =
      CollectAst.byType[Ident](ast).exists(id => leafValuedFroms.contains(id))

    // If there is one single select clause that has a primitive (i.e. Leaf) quat then we can alias it to "value"
    // This is the case of `SELECT primitive FROM (SELECT p.age from Person p) AS primitive`
    // where we turn it into `SELECT p.name AS value FROM Person p`
    def aliasSelects(selectValues: List[SelectValue]) =
      selectValues match {
        case List(sv @ SelectValue(LeafQuat(ast), _, _)) => List(sv.copy(alias = Some("value")))
        case other                                       => other
      }

    val valuizedQuery =
      q.copy(from = from)(q.quat).mapAsts { ast =>
        if (containsAlias(ast)) {
          val reductions = CollectAst.byType[Ident](ast).filter(id => leafValuedFroms.contains(id)).map(id => id -> valueize(id))
          BetaReduction(ast, TypeBehavior.ReplaceWithReduction, reductions: _*)
        } else {
          ast
        }
      }

    valuizedQuery.copy(select = aliasSelects(valuizedQuery.select))(q.quat)
  }

  // Turn every `FROM primitive-x` into a `FROM case-class(x.primitive)`
  override protected def expandContext(s: FromContext): FromContext =
    super.expandContext(s) match {
      case QueryContext(query, LeafQuat(id: Ident)) =>
        QueryContext(query, productize(id))
      case other =>
        other
    }

  // protected def expandContext(s: FromContext): FromContext =
  //   s match {
  //     case QueryContext(q, alias) =>
  //       QueryContext(apply(q, QueryLevel.Inner), alias)
  //     case JoinContext(t, a, b, on) =>
  //       JoinContext(t, expandContext(a), expandContext(b), on)
  //     case FlatJoinContext(t, a, on) =>
  //       FlatJoinContext(t, expandContext(a), on)
  //     case _: TableContext | _: InfixContext => s
  //   }

  private def collectAliases(contexts: List[FromContext]): List[Ident] =
    contexts.flatMap {
      case c: TableContext             => List(c.alias)
      case c: QueryContext             => List(c.alias)
      case c: InfixContext             => List(c.alias)
      case JoinContext(_, a, b, _)     => collectAliases(List(a)) ++ collectAliases(List(b))
      case FlatJoinContext(_, from, _) => collectAliases(List(from))
    }
}

/**
 * Remove aliases at the top level of the AST since they are not needed (quill
 * uses select row indexes to figure out what data corresponds to what encodable
 * object) as well as entities whose aliases are the same as their selection
 * e.g. "select x.foo as foo" since this just adds syntactic noise.
 */
case class RemoveExtraAlias(strategy: NamingStrategy) extends StatelessQueryTransformer {
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

  override protected def expandNested(q: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery = {
    val from   = q.from.map(expandContext(_))
    val select = q.select.map(removeUnneededAlias(_))
    q.copy(select = select, from = from)(q.quat)
  }
}
