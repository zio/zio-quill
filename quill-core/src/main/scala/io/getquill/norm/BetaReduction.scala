package io.getquill.norm

import io.getquill.ast._

case class BetaReduction(map: collection.Map[Ident, Ast])
    extends StatelessTransformer {

  override def apply(ast: Ast) =
    ast match {
      case Property(Tuple(values), name) =>
        apply(values(name.drop(1).toInt - 1))
      case FunctionApply(Function(params, body), values) =>
        apply(BetaReduction(map ++ params.zip(values)).apply(body))
      case ident: Ident =>
        map.getOrElse(ident, ident)
      case Function(params, body) =>
        Function(params, BetaReduction(map -- params)(body))
      case other =>
        super.apply(other)
    }

  override def apply(query: Query) =
    query match {
      case Filter(a, b, c) =>
        Filter(apply(a), b, BetaReduction(map - b)(c))
      case Map(a, b, c) =>
        Map(apply(a), b, BetaReduction(map - b)(c))
      case FlatMap(a, b, c) =>
        FlatMap(apply(a), b, BetaReduction(map - b)(c))
      case SortBy(a, b, c) =>
        SortBy(apply(a), b, BetaReduction(map - b)(c))
      case GroupBy(a, b, c) =>
        GroupBy(apply(a), b, BetaReduction(map - b)(c))
      case _: Reverse | _: Take | _: Entity | _: Drop | _: Union | _: UnionAll =>
        super.apply(query)
    }
}

object BetaReduction {

  def apply(ast: Ast, t: (Ident, Ast)*): Ast =
    BetaReduction(t.toMap)(ast) match {
      case `ast` => ast
      case other => apply(other)
    }
}
