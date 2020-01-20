package io.getquill.ast

trait StatelessTransformer {

  def apply(e: Ast): Ast =
    e match {
      case e: Query               => apply(e)
      case e: Operation           => apply(e)
      case e: Action              => apply(e)
      case e: Value               => apply(e)
      case e: Assignment          => apply(e)
      case Function(params, body) => Function(params, apply(body))
      case e: Ident               => e
      case e: ExternalIdent       => e
      case e: Property            => apply(e)
      case Infix(a, b, pure)      => Infix(a, b.map(apply), pure)
      case e: OptionOperation     => apply(e)
      case e: IterableOperation   => apply(e)
      case If(a, b, c)            => If(apply(a), apply(b), apply(c))
      case e: Dynamic             => e
      case e: Lift                => e
      case e: QuotedReference     => e
      case Block(statements)      => Block(statements.map(apply))
      case Val(name, body)        => Val(name, apply(body))
      case o: Ordering            => o
      case e: OnConflict.Excluded => e
      case e: OnConflict.Existing => e
    }

  def apply(o: OptionOperation): OptionOperation =
    o match {
      case OptionTableFlatMap(a, b, c) => OptionTableFlatMap(apply(a), b, apply(c))
      case OptionTableMap(a, b, c)     => OptionTableMap(apply(a), b, apply(c))
      case OptionTableExists(a, b, c)  => OptionTableExists(apply(a), b, apply(c))
      case OptionTableForall(a, b, c)  => OptionTableForall(apply(a), b, apply(c))
      case OptionFlatten(a)            => OptionFlatten(apply(a))
      case OptionGetOrElse(a, b)       => OptionGetOrElse(apply(a), apply(b))
      case OptionFlatMap(a, b, c)      => OptionFlatMap(apply(a), b, apply(c))
      case OptionMap(a, b, c)          => OptionMap(apply(a), b, apply(c))
      case OptionForall(a, b, c)       => OptionForall(apply(a), b, apply(c))
      case OptionExists(a, b, c)       => OptionExists(apply(a), b, apply(c))
      case OptionContains(a, b)        => OptionContains(apply(a), apply(b))
      case OptionIsEmpty(a)            => OptionIsEmpty(apply(a))
      case OptionNonEmpty(a)           => OptionNonEmpty(apply(a))
      case OptionIsDefined(a)          => OptionIsDefined(apply(a))
      case OptionSome(a)               => OptionSome(apply(a))
      case OptionApply(a)              => OptionApply(apply(a))
      case OptionOrNull(a)             => OptionOrNull(apply(a))
      case OptionGetOrNull(a)          => OptionGetOrNull(apply(a))
      case OptionNone                  => OptionNone
    }

  def apply(o: IterableOperation): IterableOperation =
    o match {
      case MapContains(a, b)  => MapContains(apply(a), apply(b))
      case SetContains(a, b)  => SetContains(apply(a), apply(b))
      case ListContains(a, b) => ListContains(apply(a), apply(b))
    }

  def apply(e: Query): Query =
    e match {
      case e: Entity          => e
      case Filter(a, b, c)    => Filter(apply(a), b, apply(c))
      case Map(a, b, c)       => Map(apply(a), b, apply(c))
      case FlatMap(a, b, c)   => FlatMap(apply(a), b, apply(c))
      case ConcatMap(a, b, c) => ConcatMap(apply(a), b, apply(c))
      case SortBy(a, b, c, d) => SortBy(apply(a), b, apply(c), d)
      case GroupBy(a, b, c)   => GroupBy(apply(a), b, apply(c))
      case Aggregation(o, a)  => Aggregation(o, apply(a))
      case Take(a, b)         => Take(apply(a), apply(b))
      case Drop(a, b)         => Drop(apply(a), apply(b))
      case Union(a, b)        => Union(apply(a), apply(b))
      case UnionAll(a, b)     => UnionAll(apply(a), apply(b))
      case Join(t, a, b, iA, iB, on) =>
        Join(t, apply(a), apply(b), iA, iB, apply(on))
      case FlatJoin(t, a, iA, on) =>
        FlatJoin(t, apply(a), iA, apply(on))
      case Distinct(a) => Distinct(apply(a))
      case Nested(a)   => Nested(apply(a))
    }

  def apply(e: Assignment): Assignment =
    e match {
      case Assignment(a, b, c) => Assignment(a, apply(b), apply(c))
    }

  def apply(e: Property): Property =
    e match {
      case Property.Opinionated(a, name, renameable, visibility) => Property.Opinionated(apply(a), name, renameable, visibility)
    }

  def apply(e: Operation): Operation =
    e match {
      case UnaryOperation(o, a)            => UnaryOperation(o, apply(a))
      case BinaryOperation(a, b, c)        => BinaryOperation(apply(a), b, apply(c))
      case FunctionApply(function, values) => FunctionApply(apply(function), values.map(apply))
    }

  def apply(e: Value): Value =
    e match {
      case e: Constant   => e
      case NullValue     => NullValue
      case Tuple(values) => Tuple(values.map(apply))
      case CaseClass(tuples) => {
        val (keys, values) = tuples.unzip
        CaseClass(keys.zip(values.map(apply)))
      }
    }

  def apply(e: Action): Action =
    e match {
      case Update(query, assignments)                 => Update(apply(query), assignments.map(apply))
      case Insert(query, assignments)                 => Insert(apply(query), assignments.map(apply))
      case Delete(query)                              => Delete(apply(query))
      case Returning(query, alias, property)          => Returning(apply(query), alias, apply(property))
      case ReturningGenerated(query, alias, property) => ReturningGenerated(apply(query), alias, apply(property))
      case Foreach(query, alias, body)                => Foreach(apply(query), alias, apply(body))
      case OnConflict(query, target, action)          => OnConflict(apply(query), apply(target), apply(action))
    }

  def apply(e: OnConflict.Target): OnConflict.Target =
    e match {
      case OnConflict.NoTarget          => e
      case OnConflict.Properties(props) => OnConflict.Properties(props.map(apply))
    }

  def apply(e: OnConflict.Action): OnConflict.Action =
    e match {
      case OnConflict.Ignore          => e
      case OnConflict.Update(assigns) => OnConflict.Update(assigns.map(apply))
    }

}
