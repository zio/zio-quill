package io.getquill.ast

trait StatefulTransformer[T] {

  val state: T

  def apply(e: Ast): (Ast, StatefulTransformer[T]) =
    e match {
      case e: Query     => apply(e)
      case e: Operation => apply(e)
      case e: Action    => apply(e)
      case e: Value     => apply(e)

      case e: Ident     => (e, this)

      case Function(a, b) =>
        val (bt, btt) = apply(b)
        (Function(a, bt), btt)

      case Property(a, b) =>
        val (at, att) = apply(a)
        (Property(at, b), att)

      case Infix(a, b) =>
        val (bt, btt) = apply(b)(_.apply)
        (Infix(a, bt), btt)

      case OptionOperation(t, a, b, c) =>
        val (at, att) = apply(a)
        val (ct, ctt) = att.apply(c)
        (OptionOperation(t, at, b, ct), ctt)

      case If(a, b, c) =>
        val (at, att) = apply(a)
        val (bt, btt) = att.apply(b)
        val (ct, ctt) = btt.apply(c)
        (If(at, bt, ct), ctt)

      case l: Dynamic => (l, this)
    }

  def apply(e: Query): (Query, StatefulTransformer[T]) =
    e match {
      case e: Entity => (e, this)
      case Filter(a, b, c) =>
        val (at, att) = apply(a)
        val (ct, ctt) = att.apply(c)
        (Filter(at, b, ct), ctt)
      case Map(a, b, c) =>
        val (at, att) = apply(a)
        val (ct, ctt) = att.apply(c)
        (Map(at, b, ct), ctt)
      case FlatMap(a, b, c) =>
        val (at, att) = apply(a)
        val (ct, ctt) = att.apply(c)
        (FlatMap(at, b, ct), ctt)
      case SortBy(a, b, c, d) =>
        val (at, att) = apply(a)
        val (ct, ctt) = att.apply(c)
        (SortBy(at, b, ct, d), ctt)
      case GroupBy(a, b, c) =>
        val (at, att) = apply(a)
        val (ct, ctt) = att.apply(c)
        (GroupBy(at, b, ct), ctt)
      case Aggregation(o, a) =>
        val (at, att) = apply(a)
        (Aggregation(o, at), att)
      case Take(a, b) =>
        val (at, att) = apply(a)
        val (bt, btt) = att.apply(b)
        (Take(at, bt), btt)
      case Drop(a, b) =>
        val (at, att) = apply(a)
        val (bt, btt) = att.apply(b)
        (Drop(at, bt), btt)
      case Union(a, b) =>
        val (at, att) = apply(a)
        val (bt, btt) = att.apply(b)
        (Union(at, bt), btt)
      case UnionAll(a, b) =>
        val (at, att) = apply(a)
        val (bt, btt) = att.apply(b)
        (UnionAll(at, bt), btt)
      case Join(t, a, b, iA, iB, on) =>
        val (at, att) = apply(a)
        val (bt, btt) = att.apply(b)
        val (ont, ontt) = btt.apply(on)
        (Join(t, at, bt, iA, iB, ont), ontt)
    }

  def apply(e: Operation): (Operation, StatefulTransformer[T]) =
    e match {
      case UnaryOperation(o, a) =>
        val (at, att) = apply(a)
        (UnaryOperation(o, at), att)
      case BinaryOperation(a, b, c) =>
        val (at, att) = apply(a)
        val (ct, ctt) = att.apply(c)
        (BinaryOperation(at, b, ct), ctt)
      case FunctionApply(a, b) =>
        val (at, att) = apply(a)
        val (bt, btt) = att.apply(b)(_.apply)
        (FunctionApply(at, bt), btt)
    }

  def apply(e: Value): (Value, StatefulTransformer[T]) =
    e match {
      case e: Constant => (e, this)
      case NullValue   => (e, this)
      case Tuple(a) =>
        val (at, att) = apply(a)(_.apply)
        (Tuple(at), att)
      case Set(a) =>
        val (at, att) = apply(a)(_.apply)
        (Set(at), att)
    }

  def apply(e: Action): (Action, StatefulTransformer[T]) =
    e match {
      case AssignedAction(a, b) =>
        val (at, att) = apply(a)
        val (bt, btt) = att.apply(b)(_.apply)
        (AssignedAction(at, bt), btt)
      case Update(a) =>
        val (at, att) = apply(a)
        (Update(at), att)
      case Insert(a) =>
        val (at, att) = apply(a)
        (Insert(at), att)
      case Delete(a) =>
        val (at, att) = apply(a)
        (Delete(at), att)
    }

  def apply(e: Assignment): (Assignment, StatefulTransformer[T]) =
    e match {
      case Assignment(a, b, c) =>
        val (ct, ctt) = apply(c)
        (Assignment(a, b, ct), ctt)
    }

  def apply[U, R](list: List[U])(f: StatefulTransformer[T] => U => (R, StatefulTransformer[T])) =
    list.foldLeft((List[R](), this)) {
      case ((values, t), v) =>
        val (vt, vtt) = f(t)(v)
        (values :+ vt, vtt)
    }
}
