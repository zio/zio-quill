package io.getquill.sources.sql

import io.getquill.ast._

private[sources] case class BindVariables(state: (List[Ident], List[Ident]))
  extends StatefulTransformer[(List[Ident], List[Ident])] {

  override def apply(ast: Ast) =
    ast match {
      case (i: Ident) =>
        val (vars, bindings) = state
        vars.contains(i) match {
          case true  => (Ident("?"), BindVariables((vars, bindings :+ i)))
          case false => (i, this)
        }
      case (b: RuntimeBinding) =>
        val (vars, bindings) = state
        (Ident("?"), BindVariables((vars, bindings :+ Ident(b.name))))
      case other => super.apply(ast)
    }

  override def apply(e: Query) =
    e match {
      case Take(Drop(a, b), c) =>
        val (ct, ctt) = apply(c)
        val (bt, btt) = ctt.apply(b)
        val (at, att) = btt.apply(a)
        (Take(Drop(at, bt), ct), att)
      case Map(a, b, c) =>
        val (ct, ctt) = apply(c)
        val (at, att) = ctt.apply(a)
        (Map(at, b, ct), att)
      case other =>
        super.apply(other)
    }

  override def apply(e: Action) =
    e match {
      case AssignedAction(Update(Filter(table: Entity, x, where)), assignments) =>
        val (at, att) = apply(assignments)(_.apply)
        val (wt, wtt) = att.apply(where)
        (AssignedAction(Update(Filter(table, x, wt)), at), wtt)
      case AssignedAction(Insert(table: Entity), assignments) =>
        val assignmentsWithoutGenerated =
          table
            .generated
            .fold(assignments)(generated => assignments.filterNot(_.property == generated))
        val (at, att) = apply(assignmentsWithoutGenerated)(_.apply)
        (AssignedAction(Insert(table), at), att)
      case other =>
        super.apply(other)
    }
}

private[sources] object BindVariables {

  def apply(ast: Ast, idents: List[Ident]): (Ast, List[Ident]) =
    (new BindVariables((idents, Nil))(ast)) match {
      case (ast, transformer) =>
        transformer.state match {
          case (_, bindings) => (ast, bindings)
        }
    }
}
