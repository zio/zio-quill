package io.getquill.quotation

import io.getquill.ast._
import io.getquill.util.Text

import collection.immutable.Set

case class State(seen: Set[Ident], free: Set[Ident])

case class FreeVariables(state: State) extends StatefulTransformer[State] {

  override def apply(ast: Ast): (Ast, StatefulTransformer[State]) =
    ast match {
      case ident: Ident if (!state.seen.contains(ident)) =>
        (ident, FreeVariables(State(state.seen, state.free + ident)))
      case f @ Function(params, body) =>
        val (_, t) = FreeVariables(State(state.seen ++ params, state.free))(body)
        (f, FreeVariables(State(state.seen, state.free ++ t.state.free)))
      case q @ Foreach(a, b, c) =>
        (q, free(a, b, c))
      case other =>
        super.apply(other)
    }

  override def apply(o: OptionOperation): (OptionOperation, StatefulTransformer[State]) =
    o match {
      case q @ OptionTableFlatMap(a, b, c) =>
        (q, free(a, b, c))
      case q @ OptionTableMap(a, b, c) =>
        (q, free(a, b, c))
      case q @ OptionTableExists(a, b, c) =>
        (q, free(a, b, c))
      case q @ OptionTableForall(a, b, c) =>
        (q, free(a, b, c))
      case q @ OptionFlatMap(a, b, c) =>
        (q, free(a, b, c))
      case q @ OptionMap(a, b, c) =>
        (q, free(a, b, c))
      case q @ OptionForall(a, b, c) =>
        (q, free(a, b, c))
      case q @ OptionExists(a, b, c) =>
        (q, free(a, b, c))
      case q @ FilterIfDefined(a, b, c) =>
        (q, free(a, b, c))
      case other =>
        super.apply(other)
    }

  override def apply(e: Assignment): (Assignment, StatefulTransformer[State]) =
    e match {
      case Assignment(a, b, c) =>
        val t         = FreeVariables(State(state.seen + a, state.free))
        val (bt, btt) = t(b)
        val (ct, ctt) = t(c)
        (Assignment(a, bt, ct), FreeVariables(State(state.seen, state.free ++ btt.state.free ++ ctt.state.free)))
    }

  override def apply(e: AssignmentDual): (AssignmentDual, StatefulTransformer[State]) =
    e match {
      case AssignmentDual(a1, a2, b, c) =>
        val t         = FreeVariables(State(state.seen + a1 + a2, state.free))
        val (bt, btt) = t(b)
        val (ct, ctt) = t(c)
        (
          AssignmentDual(a1, a2, bt, ct),
          FreeVariables(State(state.seen, state.free ++ btt.state.free ++ ctt.state.free))
        )
    }

  override def apply(action: Action): (Action, StatefulTransformer[State]) =
    action match {
      case q @ Returning(a, b, c) =>
        (q, free(a, b, c))
      case q @ ReturningGenerated(a, b, c) =>
        (q, free(a, b, c))
      case other =>
        super.apply(other)
    }

  override def apply(e: OnConflict.Target): (OnConflict.Target, StatefulTransformer[State]) = (e, this)

  override def apply(query: Query): (Query, StatefulTransformer[State]) =
    query match {
      case q @ Filter(a, b, c)           => (q, free(a, b, c))
      case q @ Map(a, b, c)              => (q, free(a, b, c))
      case q @ DistinctOn(a, b, c)       => (q, free(a, b, c))
      case q @ FlatMap(a, b, c)          => (q, free(a, b, c))
      case q @ ConcatMap(a, b, c)        => (q, free(a, b, c))
      case q @ SortBy(a, b, c, d)        => (q, free(a, b, c))
      case q @ GroupBy(a, b, c)          => (q, free(a, b, c))
      case q @ GroupByMap(a, b, c, d, e) =>
        // First search for free variables in the groupBy's `by` clause, then search for them in the `to` clause
        // if any were found int he `by` clause, propagate them forward to the to-clause
        val s1 = free(a, b, c)
        val s2 = new FreeVariables(s1.state).free(a, d, e)
        (q, s2)
      case q @ FlatJoin(t, a, b, c) => (q, free(a, b, c))
      case q @ Join(t, a, b, iA, iB, on) =>
        val (_, freeA)  = apply(a)
        val (_, freeB)  = apply(b)
        val (_, freeOn) = FreeVariables(State(state.seen + iA + iB, Set.empty))(on)
        (q, FreeVariables(State(state.seen, state.free ++ freeA.state.free ++ freeB.state.free ++ freeOn.state.free)))
      case _: Entity | _: Take | _: Drop | _: Union | _: UnionAll | _: Aggregation | _: Distinct | _: Nested =>
        super.apply(query)
    }

  private def free(a: Ast, ident: Ident, c: Ast) = {
    val (_, ta) = apply(a)
    val (_, tc) = FreeVariables(State(state.seen + ident, state.free))(c)
    FreeVariables(State(state.seen, state.free ++ ta.state.free ++ tc.state.free))
  }
}

case class FreeVariableError(freeVars: List[Ident]) extends Exception {
  lazy val msg = Text.FreeVariablesExitError(freeVars, true)
  // For compile-time flows where the position is already passed down the the compiler do
  // showing it again would just cause confusion
  lazy val msgNoPos = Text.FreeVariablesExitError(freeVars, false)

  override def getMessage: String = msg
}

object FreeVariables {
  def apply(ast: Ast): Set[Ident] =
    new FreeVariables(State(Set.empty, Set.empty))(ast) match {
      case (_, transformer) =>
        transformer.state.free
    }

  def verify(ast: Ast): Either[FreeVariableError, Ast] =
    apply(ast) match {
      case free if free.isEmpty => Right(ast)
      case free =>
        val error =
          FreeVariableError(free.toList)
        Left(error)
    }
}
