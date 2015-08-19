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

  def apply(ast: Ast)(implicit vars: List[Ident]): (Ast, List[Ident]) =
    ast match {
      case ast: Query =>
        apply(ast)
      case ast: Ref =>
        apply(ast)
      case UnaryOperation(op, ast) =>
        val (er, erv) = apply(ast)
        (UnaryOperation(op, er), erv)
      case BinaryOperation(a, op, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (BinaryOperation(ar, op, br), arv ++ brv)
    }

  def apply(query: Query)(implicit vars: List[Ident]): (Query, List[Ident]) =
    query match {
      case FlatMap(q, x, p) =>
        val (qr, qrv) = apply(q)
        val (pr, prv) = apply(p)
        (FlatMap(qr, x, pr), qrv ++ prv)
      case Map(q, x, p) =>
        val (qr, qrv) = apply(q)
        val (pr, prv) = apply(p)
        (Map(qr, x, pr), qrv ++ prv)
      case Filter(q, x, p) =>
        val (qr, qrv) = apply(q)
        val (pr, prv) = apply(p)
        (Filter(qr, x, pr), qrv ++ prv)
      case t: Table => (t, List())
    }

  def apply(ref: Ref)(implicit vars: List[Ident]): (Ast, List[Ident]) =
    ref match {
      case Tuple(values) =>
        val vr = values.map(apply)
        (Tuple(vr.map(_._1)), vr.map(_._2).flatten)
      case ident: Ident if (vars.contains(ident)) =>
        (Ident("?"), List(ident))
      case other => (other, List())
    }
}