package io.getquill.ast

object StatefulTransformerWithStack {
  sealed trait History {
    def ast: Option[Ast]
  }
  object History {
    case object Root extends History { val ast = None }
    case class Child(clause: Ast, prev: History) extends History {
      def ast = Some(clause)
    }

    def apply(ast: Ast)(implicit prev: History): Child =
      History.Child(ast, prev)
  }
}

trait StatefulTransformerWithStack[T] {
  import StatefulTransformerWithStack.History

  val state: T

  def apply(e: Ast)(implicit parent: History): (Ast, StatefulTransformerWithStack[T]) =
    e match {
      case e: Query               => apply(e)
      case e: Operation           => apply(e)
      case e: Action              => apply(e)
      case e: Value               => apply(e)
      case e: Assignment          => apply(e)
      case e: AssignmentDual      => apply(e)
      case e: Ident               => (e, this)
      case e: ExternalIdent       => (e, this)
      case e: OptionOperation     => apply(e)
      case e: IterableOperation   => apply(e)
      case e: Property            => apply(e)
      case e: OnConflict.Existing => (e, this)
      case e: OnConflict.Excluded => (e, this)

      case Function(a, b) =>
        val (bt, btt) = apply(b)(History(e))
        (Function(a, bt), btt)

      case Infix(a, b, pure, quat) =>
        val (bt, btt) = apply(b)(s => (u => s.apply(u)(History(e))))
        (Infix(a, bt, pure, quat), btt)

      case If(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(History(e))
        val (ct, ctt) = btt.apply(c)(History(e))
        (If(at, bt, ct), ctt)

      case l: Dynamic  => (l, this)

      case l: External => (l, this)

      case QuotedReference(a, b) =>
        val (bt, btt) = apply(b)(History(e))
        (QuotedReference(a, bt), btt)

      case Block(a) =>
        val (at, att) = apply(a)(s => (u => s.apply(u)(History(e))))
        (Block(at), att)

      case Val(a, b) =>
        val (at, att) = apply(b)(History(e))
        (Val(a, at), att)

      case o: Ordering => (o, this)
    }

  def apply(o: OptionOperation)(implicit parent: History): (OptionOperation, StatefulTransformerWithStack[T]) =
    o match {
      case OptionTableFlatMap(a, b, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionTableFlatMap(at, b, ct), ctt)
      case OptionTableMap(a, b, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionTableMap(at, b, ct), ctt)
      case OptionTableExists(a, b, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionTableExists(at, b, ct), ctt)
      case OptionTableForall(a, b, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionTableForall(at, b, ct), ctt)
      case OptionFlatten(a) =>
        val (at, att) = apply(a)(History(o))
        (OptionFlatten(at), att)
      case OptionGetOrElse(a, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionGetOrElse(at, ct), ctt)
      case OptionFlatMap(a, b, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionFlatMap(at, b, ct), ctt)
      case OptionMap(a, b, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionMap(at, b, ct), ctt)
      case OptionForall(a, b, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionForall(at, b, ct), ctt)
      case OptionExists(a, b, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionExists(at, b, ct), ctt)
      case OptionContains(a, c) =>
        val (at, att) = apply(a)(History(o))
        val (ct, ctt) = att.apply(c)(History(o))
        (OptionContains(at, ct), ctt)
      case OptionIsEmpty(a) =>
        val (at, att) = apply(a)(History(o))
        (OptionIsEmpty(at), att)
      case OptionNonEmpty(a) =>
        val (at, att) = apply(a)(History(o))
        (OptionNonEmpty(at), att)
      case OptionIsDefined(a) =>
        val (at, att) = apply(a)(History(o))
        (OptionIsDefined(at), att)
      case OptionSome(a) =>
        val (at, att) = apply(a)(History(o))
        (OptionSome(at), att)
      case OptionApply(a) =>
        val (at, att) = apply(a)(History(o))
        (OptionApply(at), att)
      case OptionOrNull(a) =>
        val (at, att) = apply(a)(History(o))
        (OptionOrNull(at), att)
      case OptionGetOrNull(a) =>
        val (at, att) = apply(a)(History(o))
        (OptionGetOrNull(at), att)
      case OptionNone(_) => (o, this)
    }

  def apply(e: IterableOperation)(implicit parent: History): (IterableOperation, StatefulTransformerWithStack[T]) =
    e match {
      case MapContains(a, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (MapContains(at, ct), ctt)
      case SetContains(a, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (SetContains(at, ct), ctt)
      case ListContains(a, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (ListContains(at, ct), ctt)
    }

  def apply(e: Query)(implicit parent: History): (Query, StatefulTransformerWithStack[T]) =
    e match {
      case e: Entity => (e, this)
      case Filter(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (Filter(at, b, ct), ctt)
      case Map(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (Map(at, b, ct), ctt)
      case FlatMap(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (FlatMap(at, b, ct), ctt)
      case ConcatMap(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (ConcatMap(at, b, ct), ctt)
      case SortBy(a, b, c, d) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (SortBy(at, b, ct, d), ctt)
      case GroupBy(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (GroupBy(at, b, ct), ctt)
      case Aggregation(o, a) =>
        val (at, att) = apply(a)(History(e))
        (Aggregation(o, at), att)
      case Take(a, b) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(History(e))
        (Take(at, bt), btt)
      case Drop(a, b) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(History(e))
        (Drop(at, bt), btt)
      case Union(a, b) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(History(e))
        (Union(at, bt), btt)
      case UnionAll(a, b) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(History(e))
        (UnionAll(at, bt), btt)
      case Join(t, a, b, iA, iB, on) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(History(e))
        val (ont, ontt) = btt.apply(on)(History(e))
        (Join(t, at, bt, iA, iB, ont), ontt)
      case FlatJoin(t, a, iA, on) =>
        val (at, att) = apply(a)(History(e))
        val (ont, ontt) = att.apply(on)(History(e))
        (FlatJoin(t, at, iA, ont), ontt)
      case Distinct(a) =>
        val (at, att) = apply(a)(History(e))
        (Distinct(at), att)
      case Nested(a) =>
        val (at, att) = apply(a)(History(e))
        (Nested(at), att)
    }

  def apply(e: Assignment)(implicit parent: History): (Assignment, StatefulTransformerWithStack[T]) =
    e match {
      case Assignment(a, b, c) =>
        val (bt, btt) = apply(b)(History(e))
        val (ct, ctt) = btt.apply(c)(History(e))
        (Assignment(a, bt, ct), ctt)
    }

  def apply(e: AssignmentDual)(implicit parent: History): (AssignmentDual, StatefulTransformerWithStack[T]) =
    e match {
      case AssignmentDual(a1, a2, b, c) =>
        val (bt, btt) = apply(b)(History(e))
        val (ct, ctt) = btt.apply(c)(History(e))
        (AssignmentDual(a1, a2, bt, ct), ctt)
    }

  def apply(e: Property)(implicit parent: History): (Property, StatefulTransformerWithStack[T]) =
    e match {
      case Property.Opinionated(a, b, renameable, visibility) =>
        val (at, att) = apply(a)(History(e))
        (Property.Opinionated(at, b, renameable, visibility), att)
    }

  def apply(e: Operation)(implicit parent: History): (Operation, StatefulTransformerWithStack[T]) =
    e match {
      case UnaryOperation(o, a) =>
        val (at, att) = apply(a)(History(e))
        (UnaryOperation(o, at), att)
      case BinaryOperation(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (BinaryOperation(at, b, ct), ctt)
      case FunctionApply(a, b) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(s => (u => s.apply(u)(History(e))))
        (FunctionApply(at, bt), btt)
    }

  def apply(e: Value)(implicit parent: History): (Value, StatefulTransformerWithStack[T]) =
    e match {
      case e: Constant => (e, this)
      case NullValue   => (e, this)
      case Tuple(a) =>
        val (at, att) = apply(a)(s => (u => s.apply(u)(History(e))))
        (Tuple(at), att)
      case CaseClass(a) =>
        val (keys, values) = a.unzip
        val (at, att) = apply(values)(s => (u => s.apply(u)(History(e))))
        (CaseClass(keys.zip(at)), att)
    }

  def apply(e: Action)(implicit parent: History): (Action, StatefulTransformerWithStack[T]) =
    e match {
      case Insert(a, b) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(s => (u => s.apply(u)(History(e))))
        (Insert(at, bt), btt)
      case Update(a, b) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(s => (u => s.apply(u)(History(e))))
        (Update(at, bt), btt)
      case Delete(a) =>
        val (at, att) = apply(a)(History(e))
        (Delete(at), att)
      case Returning(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (Returning(at, b, ct), ctt)
      case ReturningGenerated(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (ReturningGenerated(at, b, ct), ctt)
      case Foreach(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (ct, ctt) = att.apply(c)(History(e))
        (Foreach(at, b, ct), ctt)
      case OnConflict(a, b, c) =>
        val (at, att) = apply(a)(History(e))
        val (bt, btt) = att.apply(b)(History(e))
        val (ct, ctt) = btt.apply(c)(History(e))
        (OnConflict(at, bt, ct), ctt)
    }

  def apply(e: OnConflict.Target)(implicit parent: History): (OnConflict.Target, StatefulTransformerWithStack[T]) =
    e match {
      case OnConflict.NoTarget => (e, this)
      case OnConflict.Properties(a) =>
        val (at, att) = apply(a)(_.apply)
        (OnConflict.Properties(at), att)
    }

  def apply(e: OnConflict.Action)(implicit parent: History): (OnConflict.Action, StatefulTransformerWithStack[T]) =
    e match {
      case OnConflict.Ignore => (e, this)
      case OnConflict.Update(a) =>
        val (at, att) = apply(a)(_.apply)
        (OnConflict.Update(at), att)
    }

  def apply[U, R](list: List[U])(f: StatefulTransformerWithStack[T] => U => (R, StatefulTransformerWithStack[T]))(implicit parent: History) =
    list.foldLeft((List[R](), this)) {
      case ((values, t), v) =>
        val (vt, vtt) = f(t)(v)
        (values :+ vt, vtt)
    }
}
