package io.getquill.quotation

import io.getquill.ast._
import io.getquill.ast.Implicits._
import collection.immutable.Set

case class State(seen: Set[IdentName], free: Set[IdentName])

case class FreeVariables(state: State) extends StatefulTransformer[State] {

  override def apply(ast: Ast): (Ast, StatefulTransformer[State]) =
    ast match {
      case ident: Ident if (!state.seen.contains(ident.idName)) =>
        (ident, FreeVariables(State(state.seen, state.free + ident.idName)))
      case f @ Function(params, body) =>
        val (_, t) = FreeVariables(State(state.seen ++ params.map(_.idName), state.free))(body)
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
        val t         = FreeVariables(State(state.seen + a.idName, state.free))
        val (bt, btt) = t(b)
        val (ct, ctt) = t(c)
        (Assignment(a, bt, ct), FreeVariables(State(state.seen, state.free ++ btt.state.free ++ ctt.state.free)))
    }

  override def apply(e: AssignmentDual): (AssignmentDual, StatefulTransformer[State]) =
    e match {
      case AssignmentDual(a1, a2, b, c) =>
        val t         = FreeVariables(State(state.seen + a1.idName + a2.idName, state.free))
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
        val (_, freeOn) = FreeVariables(State(state.seen + iA.idName + iB.idName, Set.empty))(on)
        (q, FreeVariables(State(state.seen, state.free ++ freeA.state.free ++ freeB.state.free ++ freeOn.state.free)))
      case _: Entity | _: Take | _: Drop | _: Union | _: UnionAll | _: Aggregation | _: Distinct | _: Nested =>
        super.apply(query)
    }

  private def free(a: Ast, ident: Ident, c: Ast): FreeVariables =
    free(a, ident.idName, c)

  private def free(a: Ast, ident: IdentName, c: Ast) = {
    val (_, ta) = apply(a)
    val (_, tc) = FreeVariables(State(state.seen + ident, state.free))(c)
    FreeVariables(State(state.seen, state.free ++ ta.state.free ++ tc.state.free))
  }
}

object FreeVariables {
  def apply(ast: Ast): Set[IdentName] =
    new FreeVariables(State(Set.empty, Set.empty))(ast) match {
      case (_, transformer) =>
        transformer.state.free
    }

  def verify(ast: Ast): Either[String, Ast] =
    apply(ast) match {
      case free if free.isEmpty => Right(ast)
      case free =>
        val firstVar = free.headOption.map(_.name).getOrElse("someVar")
        Left(
          s"""
             |Found the following variables: ${free.map(_.name).toList} that seem to originate outside of a `quote {...}` or `run {...}` block.
             |Quotes and run blocks cannot use values outside their scope directly (with the exception of inline expressions in Scala 3).
             |In order to use runtime values in a quotation, you need to lift them, so instead
             |of this `$firstVar` do this: `lift($firstVar)`.
             |Here is a more complete example:
             |Instead of this: `def byName(n: String) = quote(query[Person].filter(_.name == n))`
             |        Do this: `def byName(n: String) = quote(query[Person].filter(_.name == lift(n)))`
        """.stripMargin
        )
    }
}
