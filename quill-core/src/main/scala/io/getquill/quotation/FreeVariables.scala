package io.getquill.quotation

import io.getquill.ast._

case class State(seen: Set[Ident], free: Set[Ident])

case class FreeVariables(state: State)
    extends StatefulTransformer[State] {

  override def apply(ast: Ast): (Ast, StatefulTransformer[State]) =
    ast match {
      case ident: Ident if (!state.seen.contains(ident)) =>
        (ident, FreeVariables(State(state.seen, state.free + ident)))
      case f @ Function(params, body) =>
        val (_, t) = FreeVariables(State(state.seen ++ params, state.free))(body)
        (f, FreeVariables(State(state.seen, state.free ++ t.state.free)))
      case other =>
        super.apply(other)
    }

  override def apply(query: Query): (Query, StatefulTransformer[State]) =
    query match {
      case q @ Filter(a, b, c)  => apply(q, a, b, c)
      case q @ Map(a, b, c)     => apply(q, a, b, c)
      case q @ FlatMap(a, b, c) => apply(q, a, b, c)
      case q @ SortBy(a, b, c)  => apply(q, a, b, c)
      case q @ GroupBy(a, b, c) => apply(q, a, b, c)
      case _: Entity | _: Reverse | _: Take | _: Drop | _: Union | _: UnionAll =>
        super.apply(query)
    }

  private def apply(q: Query, a: Ast, b: Ident, c: Ast): (Query, StatefulTransformer[State]) = {
    val (_, ta) = apply(a)
    val (_, tc) = FreeVariables(State(state.seen + b, state.free))(c)
    (q, FreeVariables(State(state.seen, state.free ++ ta.state.free ++ tc.state.free)))
  }
}

object FreeVariables {
  def apply(ast: Ast) =
    new FreeVariables(State(Set(), Set()))(ast) match {
      case (_, transformer) =>
        transformer.state.free
    }
}
