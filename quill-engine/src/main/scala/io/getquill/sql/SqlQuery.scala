package io.getquill.context.sql

import io.getquill.ast._
import io.getquill.context.sql.norm.{ ExpandSelection, FlattenGroupByAggregation }
import io.getquill.norm.BetaReduction
import io.getquill.quat.Quat
import io.getquill.util.Messages.fail
import io.getquill.{ Literal, PseudoAst }

case class OrderByCriteria(ast: Ast, ordering: PropertyOrdering)

sealed trait FromContext { def quat: Quat }
case class TableContext(entity: Entity, alias: String) extends FromContext { def quat = entity.quat }
case class QueryContext(query: SqlQuery, alias: String) extends FromContext { def quat = query.quat }
case class InfixContext(infix: Infix, alias: String) extends FromContext { def quat = infix.quat }
case class JoinContext(t: JoinType, a: FromContext, b: FromContext, on: Ast) extends FromContext { def quat = Quat.Tuple(a.quat, b.quat) }
case class FlatJoinContext(t: JoinType, a: FromContext, on: Ast) extends FromContext { def quat = a.quat }

sealed trait SqlQuery {
  def quat: Quat

  override def toString = {
    import io.getquill.MirrorSqlDialect._
    import io.getquill.idiom.StatementInterpolator._
    implicit val naming: Literal = Literal
    implicit val tokenizer: Tokenizer[Ast] = defaultTokenizer
    this.token.toString
  }
}

sealed trait SetOperation
case object UnionOperation extends SetOperation
case object UnionAllOperation extends SetOperation

sealed trait DistinctKind { def isDistinct: Boolean }
case object DistinctKind {
  case object Distinct extends DistinctKind { val isDistinct: Boolean = true }
  case class DistinctOn(props: List[Ast]) extends DistinctKind { val isDistinct: Boolean = true }
  case object None extends DistinctKind { val isDistinct: Boolean = false }
}

case class SetOperationSqlQuery(
  a:  SqlQuery,
  op: SetOperation,
  b:  SqlQuery
)(quatType: Quat) extends SqlQuery {
  def quat = quatType
}

case class UnaryOperationSqlQuery(
  op: UnaryOperator,
  q:  SqlQuery
)(quatType: Quat) extends SqlQuery {
  def quat = quatType
}

case class SelectValue(ast: Ast, alias: Option[String] = None, concat: Boolean = false) extends PseudoAst {
  override def toString: String = s"${ast.toString}${alias.map("->" + _).getOrElse("")}"
}

case class FlattenSqlQuery(
  from:     List[FromContext]     = List(),
  where:    Option[Ast]           = None,
  groupBy:  Option[Ast]           = None,
  orderBy:  List[OrderByCriteria] = Nil,
  limit:    Option[Ast]           = None,
  offset:   Option[Ast]           = None,
  select:   List[SelectValue],
  distinct: DistinctKind          = DistinctKind.None
)(quatType: Quat) extends SqlQuery {
  def quat = quatType
}

object TakeDropFlatten {
  def unapply(q: Query): Option[(Query, Option[Ast], Option[Ast])] = q match {
    case Take(q: FlatMap, n) => Some((q, Some(n), None))
    case Drop(q: FlatMap, n) => Some((q, None, Some(n)))
    case _                   => None
  }
}

object CaseClassMake {
  def fromQuat(quat: Quat)(idName: String) =
    quat match {
      case Quat.Product(fields) =>
        CaseClass(fields.toList.map { case (name, _) => (name, Property(Ident(idName, quat), name)) })
      // Figure out a way to test this case?
      case _ =>
        CaseClass(List((idName, Ident(idName, quat))))
    }
}

object SqlQuery {

  def apply(query: Ast): SqlQuery =
    query match {
      case Union(a, b)                       => SetOperationSqlQuery(apply(a), UnionOperation, apply(b))(query.quat)
      case UnionAll(a, b)                    => SetOperationSqlQuery(apply(a), UnionAllOperation, apply(b))(query.quat)
      case UnaryOperation(op, q: Query)      => UnaryOperationSqlQuery(op, apply(q))(query.quat)
      case _: Operation | _: Value           => FlattenSqlQuery(select = List(SelectValue(query)))(query.quat)
      case Map(q, a, b) if a == b            => apply(q)
      case TakeDropFlatten(q, limit, offset) => flatten(q, "x").copy(limit = limit, offset = offset)(q.quat)
      case q: Query                          => flatten(q, "x")
      case infix: Infix                      => flatten(infix, "x")
      case other                             => fail(s"Query not properly normalized. Please open a bug report. Ast: '$other'")
    }

  private def flatten(query: Ast, alias: String): FlattenSqlQuery = {
    val (sources, finalFlatMapBody) = flattenContexts(query)
    flatten(sources, finalFlatMapBody, alias)
  }

  private def flattenContexts(query: Ast): (List[FromContext], Ast) =
    query match {
      // A flat-join query with no maps e.g: `qr1.flatMap(e1 => qr1.join(e2 => e1.i == e2.i))`
      case FlatMap(q @ (_: Query | _: Infix), id: Ident, flatJoin @ FlatJoin(_, _, alias @ Ident(name, _), _)) =>
        val cc = CaseClassMake.fromQuat(flatJoin.quat)(name)
        flattenContexts(FlatMap(q, id, Map(flatJoin, alias, cc)))
      case FlatMap(q @ (_: Query | _: Infix), Ident(alias, _), p: Query) =>
        val source = this.source(q, alias)
        val (nestedContexts, finalFlatMapBody) = flattenContexts(p)
        (source +: nestedContexts, finalFlatMapBody)
      case FlatMap(q @ (_: Query | _: Infix), Ident(alias, _), p: Infix) =>
        fail(s"Infix can't be use as a `flatMap` body. $query")
      case other =>
        (List.empty, other)
    }

  private def flatten(sources: List[FromContext], finalFlatMapBody: Ast, alias: String): FlattenSqlQuery = {

    def select(alias: String, quat: Quat) = SelectValue(Ident(alias, quat), None) :: Nil

    def base(q: Ast, alias: String) = {
      def nest(ctx: FromContext) = FlattenSqlQuery(from = sources :+ ctx, select = select(alias, q.quat))(q.quat)
      q match {
        case Map(_: GroupBy, _, _) => nest(source(q, alias))
        case Nested(q)             => nest(QueryContext(apply(q), alias))
        case q: ConcatMap          => nest(QueryContext(apply(q), alias))
        case Join(tpe, a, b, iA, iB, on) =>
          val ctx = source(q, alias)
          def aliases(ctx: FromContext): List[(String, Quat)] =
            ctx match {
              case TableContext(_, alias)   => (alias, ctx.quat) :: Nil
              case QueryContext(_, alias)   => (alias, ctx.quat) :: Nil
              case InfixContext(_, alias)   => (alias, ctx.quat) :: Nil
              case JoinContext(_, a, b, _)  => aliases(a) ::: aliases(b)
              case FlatJoinContext(_, a, _) => aliases(a)
            }
          // TODO Quat what impact does it have on this process if we do the alternative version of ExpandJoins that uses nested flatmaps?
          //      would that version actually produce more consistent results here?
          // TODO Quat Test in situations where you have more then two things. Would the subselect work properly?
          // Maybe prduce nested tuples here based on if it is a join context etc... in the recursive creation of the types
          val collectedAliases = aliases(ctx).map { case (a, quat) => Ident(a, quat) }
          val select = Tuple(collectedAliases)
          FlattenSqlQuery(
            from = ctx :: Nil,
            select = List(SelectValue(select, None))
          )(q.quat)
        case q @ (_: Map | _: Filter | _: Entity) => flatten(sources, q, alias)
        case q if (sources == Nil)                => flatten(sources, q, alias)
        case other                                => nest(source(q, alias))
      }
    }

    val quat = finalFlatMapBody.quat
    finalFlatMapBody match {

      case ConcatMap(q, Ident(alias, _), p) =>
        FlattenSqlQuery(
          from = source(q, alias) :: Nil,
          select = selectValues(p).map(_.copy(concat = true))
        )(quat)

      case Map(GroupBy(q, x @ Ident(alias, _), g), a, p) =>
        val b = base(q, alias)
        //use ExpandSelection logic to break down OrderBy clause
        val flatGroupByAsts = new ExpandSelection(b.from).ofSubselect(List(SelectValue(g))).map(_.ast)
        val groupByClause =
          if (flatGroupByAsts.length > 1) Tuple(flatGroupByAsts)
          else flatGroupByAsts.head

        val select = BetaReduction(p, a -> Tuple(List(g, x)))
        val flattenSelect = FlattenGroupByAggregation(x)(select)
        b.copy(groupBy = Some(groupByClause), select = this.selectValues(flattenSelect))(quat)

      case GroupBy(q, Ident(alias, _), p) =>
        fail("A `groupBy` clause must be followed by `map`.")

      case Map(q, Ident(alias, _), p) =>
        val b = base(q, alias)
        val agg = b.select.collect {
          case s @ SelectValue(_: Aggregation, _, _) => s
        }
        if (!b.distinct.isDistinct && agg.isEmpty)
          b.copy(select = selectValues(p))(quat)
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            select = selectValues(p)
          )(quat)

      case Filter(q, Ident(alias, _), p) =>
        val b = base(q, alias)
        //If the filter body uses the filter alias, make sure it matches one of the aliases in the fromContexts
        if (b.where.isEmpty && (!CollectAst.byType[Ident](p).map(_.name).contains(alias) || collectAliases(b.from).contains(alias)))
          b.copy(where = Some(p))(quat)
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            where = Some(p),
            select = select(alias, quat)
          )(quat)

      case SortBy(q, Ident(alias, _), p, o) =>
        val b = base(q, alias)
        val criterias = orderByCriterias(p, o, b.from)
        //If the sortBy body uses the filter alias, make sure it matches one of the aliases in the fromContexts
        if (b.orderBy.isEmpty && (!CollectAst.byType[Ident](p).map(_.name).contains(alias) || collectAliases(b.from).contains(alias)))
          b.copy(orderBy = criterias)(quat)
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            orderBy = criterias,
            select = select(alias, quat)
          )(quat)

      case Aggregation(op, q: Query) =>
        val b = flatten(q, alias)
        b.select match {
          case head :: Nil if !b.distinct.isDistinct =>
            b.copy(select = List(head.copy(ast = Aggregation(op, head.ast))))(quat)
          case other =>
            FlattenSqlQuery(
              from = QueryContext(apply(q), alias) :: Nil,
              select = List(SelectValue(Aggregation(op, Ident("*", quat)))) // Quat of a * aggregation is same as for the entire query
            )(quat)
        }

      case Take(q, n) =>
        val b = base(q, alias)
        if (b.limit.isEmpty)
          b.copy(limit = Some(n))(quat)
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            limit = Some(n),
            select = select(alias, quat)
          )(quat)

      case Drop(q, n) =>
        val b = base(q, alias)
        if (b.offset.isEmpty && b.limit.isEmpty)
          b.copy(offset = Some(n))(quat)
        else
          FlattenSqlQuery(
            from = QueryContext(apply(q), alias) :: Nil,
            offset = Some(n),
            select = select(alias, quat)
          )(quat)

      case Distinct(q) =>
        val b = base(q, alias)
        b.copy(distinct = DistinctKind.Distinct)(quat)

      case DistinctOn(q, _, fields) =>
        val distinctList =
          fields match {
            case Tuple(values) => values
            case other         => List(other)
          }

        val b = base(q, alias)
        b.copy(distinct = DistinctKind.DistinctOn(distinctList))(quat)

      case other =>
        FlattenSqlQuery(from = sources :+ source(other, alias), select = select(alias, quat))(quat)
    }
  }

  private def selectValues(ast: Ast) =
    ast match {
      //case Tuple(values) => values.map(SelectValue(_))
      case other => SelectValue(ast) :: Nil
    }

  private def source(ast: Ast, alias: String): FromContext =
    ast match {
      case entity: Entity            => TableContext(entity, alias)
      case infix: Infix              => InfixContext(infix, alias)
      case Join(t, a, b, ia, ib, on) => JoinContext(t, source(a, ia.name), source(b, ib.name), on)
      case FlatJoin(t, a, ia, on)    => FlatJoinContext(t, source(a, ia.name), on)
      case Nested(q)                 => QueryContext(apply(q), alias)
      case other                     => QueryContext(apply(other), alias)
    }

  private def orderByCriterias(ast: Ast, ordering: Ast, from: List[FromContext]): List[OrderByCriteria] =
    (ast, ordering) match {
      case (Tuple(properties), ord: PropertyOrdering) => properties.flatMap(orderByCriterias(_, ord, from))
      case (Tuple(properties), TupleOrdering(ord))    => properties.zip(ord).flatMap { case (a, o) => orderByCriterias(a, o, from) }
      //if its a quat product, use ExpandSelection to break it down into its component fields and apply the ordering to all of them
      case (id @ Ident(_, _: Quat.Product), ord)      => new ExpandSelection(from).ofSubselect(List(SelectValue(ast))).map(_.ast).flatMap(orderByCriterias(_, ord, from))
      case (a, o: PropertyOrdering)                   => List(OrderByCriteria(a, o))
      case other                                      => fail(s"Invalid order by criteria $ast")
    }

  private def collectAliases(contexts: List[FromContext]): List[String] = {
    contexts.flatMap {
      case c: TableContext             => List(c.alias)
      case c: QueryContext             => List(c.alias)
      case c: InfixContext             => List(c.alias)
      case JoinContext(_, a, b, _)     => collectAliases(List(a)) ++ collectAliases(List(b))
      case FlatJoinContext(_, from, _) => collectAliases(List(from))
    }
  }

  private def collectTableAliases(contexts: List[FromContext]): List[String] = {
    contexts.flatMap {
      case c: TableContext             => List(c.alias)
      case c: QueryContext             => List()
      case c: InfixContext             => List()
      case JoinContext(_, a, b, _)     => collectAliases(List(a)) ++ collectAliases(List(b))
      case FlatJoinContext(_, from, _) => collectAliases(List(from))
    }
  }

}

