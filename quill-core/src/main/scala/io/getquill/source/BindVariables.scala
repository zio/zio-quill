package io.getquill.source

import io.getquill.ast.Action
import io.getquill.ast.Assignment
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Delete
import io.getquill.ast.Ast
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Insert
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.Ref
import io.getquill.ast.Table
import io.getquill.ast.Tuple
import io.getquill.ast.UnaryOperation
import io.getquill.ast.Update
import io.getquill.ast.Function
import io.getquill.ast.StatefulTransformer

private[source] case class BindVariables(state: (List[Ident], List[Ident]))
    extends StatefulTransformer[(List[Ident], List[Ident])] {

  override def apply(i: Ident) =
    state match {
      case (vars, bindings) if (vars.contains(i)) =>
        (Ident("?"), BindVariables((vars, bindings :+ i)))
      case other =>
        (i, this)
    }
}

private[source] object BindVariables {

  def apply(action: Action)(implicit vars: List[Ident]): (Action, List[Ident]) =
    action match {
      case Update(query, assignments) =>
        val (qr, qrv) = apply(query)
        val (ar, arv) = apply(assignments)
        (Update(qr, ar), qrv ++ arv)
      case Insert(query, assignments) =>
        val (qr, qrv) = apply(query)
        val (ar, arv) = apply(assignments)
        (Insert(qr, ar), qrv ++ arv)
      case Delete(query) =>
        val (qr, qrv) = apply(query)
        (Delete(qr), qrv)
    }

  def apply(assignments: List[Assignment])(implicit vars: List[Ident]): (List[Assignment], List[Ident]) = {
    val r = assignments.map(apply)
    (r.map(_._1), r.map(_._2).flatten)
  }

  def apply(assignment: Assignment)(implicit vars: List[Ident]): (Assignment, List[Ident]) =
    assignment match {
      case Assignment(prop, value) =>
        val (vr, vrv) = apply(value)
        (Assignment(prop, vr), vrv)
    }

  def apply(ast: Ast)(implicit vars: List[Ident]): (Ast, List[Ident]) = {
    val (astt, transformer) = BindVariables((vars, List())).apply(ast)
    (astt, transformer.state._2)
  }
}