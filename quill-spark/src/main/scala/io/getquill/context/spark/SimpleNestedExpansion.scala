package io.getquill.context.spark

import io.getquill.ast._
import io.getquill.context.sql._
import io.getquill.quat.Quat
import io.getquill.sql.norm.{QueryLevel, StatelessQueryTransformer}

object TopLevelExpansion {

  implicit class AliasOp(alias: Option[String]) {
    def concatWith(str: String): Option[String] =
      alias.orElse(Some("")).map(v => s"${v}${str}")
  }

  def apply(values: List[SelectValue], length: Int): List[SelectValue] =
    values.flatMap(apply(_, length))

  private def apply(value: SelectValue, length: Int): List[SelectValue] =
    value match {
      case SelectValue(Tuple(values), alias, concat) =>
        values.zipWithIndex.map { case (ast, i) =>
          SelectValue(ast, Some(s"_${i + 1}"), concat)
        }
      case SelectValue(CaseClass(_, fields), alias, concat) =>
        fields.map { case (name, ast) =>
          SelectValue(ast, Some(name), concat)
        }

      // If an ident is the only thing in the select list (and it is not abstract, meaning we know what all of it's fields are),
      // then expand it directly in the selection clauses. If the ident is abstract however, we do not know all of it's fields
      // and therefore cannot directly expand it into select-values since there could be fields that we have missed.
      // The only good option that I have thought of so far, is to expand the Ident in the SparkDialect directly
      // in the FlattenSqlTokenizer with special handling for length=1 selects
      case SelectValue(Ident(singleFieldName, q @ Quat.Product(fields)), alias, concat)
          if (length == 1 && q.tpe == Quat.Product.Type.Concrete) =>
        fields.map { case (name, quat) =>
          SelectValue(Property(Ident(singleFieldName, quat), name), Some(name), concat)
        }.toList

      // Direct infix select, etc...
      case other @ SelectValue(SingleValuePrimitive(), currentAlias, _) if (length == 1) =>
        List(other.copy(alias = currentAlias.orElse(Some("x"))))
      // Technically this case should not exist, adding it so that the pattern match will have full coverage
      case other =>
        List(other)
    }
}

/**
 * In each of these situations, the AST can be expanded with as a single
 * SelectValue (with the 'single' alias) in a select query. For example,
 * SelectValue(Property(p,name)) would become 'SELECT p.name AS single ...'.
 * SelectValue(If(foo,bar,baz)) would become 'SELECT CASE WHEN foo THEN bar ELSE
 * baz as single ...', or SelectValue(NullValue) would become 'SELECT null as
 * single ...' In each of these cases we can assume the actual selection
 * represents a single value. The opposite of this if the selection is an Ident.
 * In that case, we can never really assume that the selection represents a
 * single value (*) so we generally have to do star expansions of various kinds.
 * This unapplier object is used both here and in the SpartDialect select
 * tokenization.
 *
 *   - unless the Ident has a Concrete Quat.Product with a single value, but
 *     that has already been expanded into it's composite elements in
 *     TopLevelExpansion.apply and the Ident should no longer exist in the
 *     select values.
 *
 * Technically, all we we need to do here is to check that the ast element is
 * not an ident, however due to previous issues encountered with surprising
 * use-cases with spark, I have decided to ast least log a warning if a
 * single-value element in a Spark query SelectValue is not one of the expected
 * use-cases.
 */
object SingleValuePrimitive {
  def unapply(ast: Ast): Boolean =
    ast match {
      case _: Constant                                      => true
      case _: Property                                      => true
      case NullValue                                        => true
      case op: OptionOperation if (op.quat.isPrimitive)     => true
      case op: BinaryOperation if (op.quat.isPrimitive)     => true
      case op: UnaryOperation if (op.quat.isPrimitive)      => true
      case op: IterableOperation if (op.quat.isPrimitive)   => true
      case i: Infix if (!i.quat.isInstanceOf[Quat.Product]) => true
      case l: Lift                                          => true
      // idents should not be directly tokenized as sql values since they always need to be a start-select in a query
      case id: Ident => false
      case _ =>
        import io.getquill.util.Messages._
        trace(
          s"Unaccounted-for primitive SelectValue type: ${qprintCustom(traceQuats = QuatTrace.Full)(ast).plainText}. Assuming it can be used as a value-level placeholder and be aliased as 'single'.",
          traceType = TraceType.Warning
        )
        true
    }
}

object SimpleNestedExpansion extends StatelessQueryTransformer {

  protected override def apply(q: SqlQuery, level: QueryLevel): SqlQuery =
    q match {
      case q: FlattenSqlQuery => // if (isTopLevel) =>  //needs to be for all levels
        expandNested(q.copy(select = TopLevelExpansion(q.select, q.select.length))(q.quat), level)
      case other =>
        super.apply(q, level)
    }

  protected override def expandNested(q: FlattenSqlQuery, level: QueryLevel): FlattenSqlQuery =
    q match {
      case FlattenSqlQuery(from, where, groupBy, orderBy, limit, offset, select, distinct) =>
        val newFroms = q.from.map(expandContext(_))

        def distinctIfNotTopLevel(values: List[SelectValue]) =
          if (level.isTop)
            values
          else
            values.distinct

        val distinctSelects =
          distinctIfNotTopLevel(select)

        q.copy(select = distinctSelects, from = newFroms)(q.quat)
    }
}
