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

      case Function(params, body) =>
        val (bodyt, bt) = apply(body)
        (Function(params, bodyt), bt)

      case FunctionApply(function, values) =>
        val (functiont, functiontt) = apply(function)
        val (valuest, valuestt) = functiontt.apply(values)(_.apply)
        (FunctionApply(functiont, valuest), valuestt)

      case Property(a, name) =>
        val (at, att) = apply(a)
        (Property(at, name), att)
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
    }

  def apply(e: Value): (Value, StatefulTransformer[T]) =
    e match {
      case e: Constant => (e, this)
      case NullValue   => (e, this)
      case Tuple(values) =>
        val (valuest, valuestt) = apply(values)(_.apply)
        (Tuple(valuest), valuestt)
    }

  def apply(e: Action): (Action, StatefulTransformer[T]) =
    e match {
      case Update(query, assignments) =>
        val (queryt, querytt) = apply(query)
        val (at, att) = querytt.apply(assignments)(_.apply)
        (Update(queryt, at), att)
      case Insert(query, assignments) =>
        val (queryt, querytt) = apply(query)
        val (at, att) = querytt.apply(assignments)(_.apply)
        (Insert(queryt, at), att)
      case Delete(query) =>
        val (qt, qtt) = apply(query)
        (Delete(query), qtt)
    }

  def apply(e: Assignment): (Assignment, StatefulTransformer[T]) =
    e match {
      case Assignment(a, b) =>
        val (at, att) = apply(a)
        val (bt, btt) = att.apply(b)
        (Assignment(at, bt), btt)
    }

  private def apply[U, R](list: List[U])(f: StatefulTransformer[T] => U => (R, StatefulTransformer[T])) =
    list.foldLeft((List[R](), this)) {
      case ((values, t), v) =>
        val (vt, vtt) = f(t)(v)
        (values :+ vt, vtt)
    }
}
