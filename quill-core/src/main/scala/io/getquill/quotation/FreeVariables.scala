package io.getquill.quotation

import io.getquill.ast.StatefulTransformer
import io.getquill.ast._

case class FreeVariables(state: (Set[Ident], Set[Ident]))
    extends StatefulTransformer[(Set[Ident], Set[Ident])] {

  protected def seen(state: (Set[Ident], Set[Ident]) = state) =
    state match {
      case (seen, _) => seen
    }

  protected def freeVars(state: (Set[Ident], Set[Ident]) = state) =
    state match {
      case (_, freeVars) => freeVars
    }

  override def apply(ast: Ast): (Ast, StatefulTransformer[(Set[Ident], Set[Ident])]) =
    ast match {
      case ident: Ident if (!seen().contains(ident)) =>
        (ident, FreeVariables((seen(), freeVars() + ident)))
      case f @ Function(params, body) =>
        val (_, t) = FreeVariables((seen() ++ params, freeVars()))(body)
        (f, FreeVariables((seen(), freeVars() ++ freeVars(t.state))))
      case other =>
        super.apply(other)
    }

  override def apply(query: Query): (Query, StatefulTransformer[(Set[Ident], Set[Ident])]) =
    query match {
      case t: Entity =>
        (t, this)
      case q @ Filter(a, b, c) =>
        val (_, ta) = apply(a)
        val (_, tc) = FreeVariables((seen() + b, freeVars()))(c)
        (q, FreeVariables((seen(), freeVars() ++ freeVars(ta.state) ++ freeVars(tc.state))))
      case q @ Map(a, b, c) =>
        val (_, ta) = apply(a)
        val (_, tc) = FreeVariables((seen() + b, freeVars()))(c)
        (q, FreeVariables((seen(), freeVars() ++ freeVars(ta.state) ++ freeVars(tc.state))))
      case q @ FlatMap(a, b, c) =>
        val (_, ta) = apply(a)
        val (_, tc) = FreeVariables((seen() + b, freeVars()))(c)
        (q, FreeVariables((seen(), freeVars() ++ freeVars(ta.state) ++ freeVars(tc.state))))
    }

  override def apply(e: Assignment) =
    e match {
      case e @ Assignment(a, b) =>
        val (_, t) = apply(b)
        (e, t)
    }
}

object FreeVariables {
  def apply(ast: Ast) =
    new FreeVariables((Set(), Set()))(ast) match {
      case (_, transformer) =>
        transformer.state match {
          case (_, freeVars) => freeVars
        }
    }
}