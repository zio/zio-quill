package io.getquill.norm

import io.getquill.ast.Aggregation
import io.getquill.ast.Ast
import io.getquill.ast.Block
import io.getquill.ast.Distinct
import io.getquill.ast.Drop
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Function
import io.getquill.ast.FunctionApply
import io.getquill.ast.GroupBy
import io.getquill.ast.Ident
import io.getquill.ast.Join
import io.getquill.ast.Map
import io.getquill.ast.OptionOperation
import io.getquill.ast.Property
import io.getquill.ast.Query
import io.getquill.ast.SortBy
import io.getquill.ast.StatelessTransformer
import io.getquill.ast.Take
import io.getquill.ast.Tuple
import io.getquill.ast.Union
import io.getquill.ast.UnionAll
import io.getquill.ast.Val

case class BetaReduction(map: collection.Map[Ident, Ast])
  extends StatelessTransformer {

  override def apply(ast: Ast) =
    ast match {
      case Property(Tuple(values), name) =>
        val aliases = values.distinct
        aliases match {
          case alias :: Nil if values.size > 1 =>
            super.apply(Property(alias, name))
          case _ => apply(values(name.drop(1).toInt - 1))
        }
      case FunctionApply(Function(params, body), values) =>
        apply(BetaReduction(map ++ params.zip(values).toMap).apply(body))
      case ident: Ident =>
        map.get(ident).map(BetaReduction(map - ident)(_)).getOrElse(ident)
      case Function(params, body) =>
        Function(params, BetaReduction(map -- params)(body))
      case OptionOperation(t, a, b, c) =>
        OptionOperation(t, apply(a), b, BetaReduction(map - b)(c))
      case Block(statements) =>
        val vals = statements.collect { case x: Val => x.name -> x.body }
        BetaReduction(map ++ vals)(statements.last)
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
      case SortBy(a, b, c, d) =>
        SortBy(apply(a), b, BetaReduction(map - b)(c), d)
      case GroupBy(a, b, c) =>
        GroupBy(apply(a), b, BetaReduction(map - b)(c))
      case Join(t, a, b, iA, iB, on) =>
        Join(t, apply(a), apply(b), iA, iB, BetaReduction(map - iA - iB)(on))
      case _: Take | _: Entity | _: Drop | _: Union | _: UnionAll | _: Aggregation | _: Distinct =>
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
