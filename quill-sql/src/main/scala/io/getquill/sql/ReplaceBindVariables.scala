package io.getquill.sql

import io.getquill.ast._

object ReplaceBindVariables {

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

  def apply(expr: Expr)(implicit vars: List[Ident]): (Expr, List[Ident]) =
    expr match {
      case expr: Ref =>
        apply(expr)
      case Subtract(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (Subtract(ar, br), arv ++ brv)
      case Division(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (Division(ar, br), arv ++ brv)
      case Remainder(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (Remainder(ar, br), arv ++ brv)
      case Add(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (Add(ar, br), arv ++ brv)
      case Equals(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (Equals(ar, br), arv ++ brv)
      case And(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (And(ar, br), arv ++ brv)
      case GreaterThan(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (GreaterThan(ar, br), arv ++ brv)
      case GreaterThanOrEqual(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (GreaterThanOrEqual(ar, br), arv ++ brv)
      case LessThan(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (LessThan(ar, br), arv ++ brv)
      case LessThanOrEqual(a, b) =>
        val (ar, arv) = apply(a)
        val (br, brv) = apply(b)
        (LessThanOrEqual(ar, br), arv ++ brv)
    }

  def apply(ref: Ref)(implicit vars: List[Ident]): (Expr, List[Ident]) =
    ref match {
      case Tuple(values) =>
        val vr = values.map(apply)
        (Tuple(vr.map(_._1)), vr.map(_._2).flatten)
      case ident: Ident if (vars.contains(ident)) =>
        (Ident("?"), List(ident))
      case other => (other, List())
    }
}