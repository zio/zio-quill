package io.getquill.norm

import io.getquill.ast.Ast
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Function
import io.getquill.ast.FunctionApply
import io.getquill.ast.Ident
import io.getquill.ast.Map
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.Tuple
import io.getquill.ast.Reverse
import io.getquill.ast.StatelessTransformer

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
      case t: Entity => t
      case Filter(a, b, c) =>
        Filter(apply(a), b, BetaReduction(map - b)(c))
      case Map(a, b, c) =>
        Map(apply(a), b, BetaReduction(map - b)(c))
      case FlatMap(a, b, c) =>
        FlatMap(apply(a), b, BetaReduction(map - b)(c))
      case SortBy(a, b, c) =>
        SortBy(apply(a), b, BetaReduction(map - b)(c))
      case Reverse(a) =>
        Reverse(apply(a))
    }
}

object BetaReduction {

  def apply(ast: Ast, t: (Ident, Ast)*): Ast =
    BetaReduction(t.toMap)(ast)
}
