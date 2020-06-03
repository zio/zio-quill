package io.getquill.dsl

import io.getquill.ast.Renameable.Fixed

import scala.language.implicitConversions
import scala.language.experimental.macros
import io.getquill.ast._

import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.util.Messages._

import scala.annotation.tailrec
import scala.util.DynamicVariable
import scala.reflect.ClassTag

class DynamicQueryDslMacro(val c: MacroContext) {
  import c.universe._

  def dynamicUnquote(d: Tree): Tree =
    q"${c.prefix}.unquote($d.q)"

  def insertValue(value: Tree): Tree =
    q"""
      DynamicInsert(${c.prefix}.q.insert(lift($value)))
    """

  def updateValue(value: Tree): Tree =
    q"""
      DynamicUpdate(${c.prefix}.q.update(lift($value)))
    """
}

trait DynamicQueryDsl {
  dsl: CoreDsl =>

  implicit class ToDynamicQuery[T](q: Quoted[Query[T]]) {
    def dynamic: DynamicQuery[T] = DynamicQuery(q)
  }

  implicit class ToDynamicEntityQuery[T](q: Quoted[EntityQuery[T]]) {
    def dynamic: DynamicEntityQuery[T] = DynamicEntityQuery(q)
  }

  implicit class ToDynamicAction[T](q: Quoted[Action[T]]) {
    def dynamic: DynamicAction[Action[T]] = DynamicAction(q)
  }

  implicit class ToDynamicInsert[T](q: Quoted[Insert[T]]) {
    def dynamic: DynamicInsert[T] = DynamicInsert(q)
  }

  implicit class ToDynamicUpdate[T](q: Quoted[Update[T]]) {
    def dynamic: DynamicUpdate[T] = DynamicUpdate(q)
  }

  implicit class ToDynamicActionReturning[T, U](q: Quoted[ActionReturning[T, U]]) {
    def dynamic: DynamicActionReturning[T, U] = DynamicActionReturning(q)
  }

  implicit def dynamicUnquote[T](d: DynamicQuery[T]): Query[T] = macro DynamicQueryDslMacro.dynamicUnquote

  implicit def toQuoted[T](q: DynamicQuery[T]): Quoted[Query[T]] = q.q
  implicit def toQuoted[T](q: DynamicEntityQuery[T]): Quoted[EntityQuery[T]] = q.q
  implicit def toQuoted[T <: Action[_]](q: DynamicAction[T]): Quoted[T] = q.q

  def dynamicQuery[T](implicit t: ClassTag[T]): DynamicEntityQuery[T] =
    DynamicEntityQuery(splice[EntityQuery[T]](
      Entity(t.runtimeClass.getName.split('.').last.split('$').last, Nil)
    ))

  case class DynamicAlias[T](property: Quoted[T] => Quoted[Any], name: String)

  def alias[T](property: Quoted[T] => Quoted[Any], name: String): DynamicAlias[T] = DynamicAlias(property, name)

  sealed trait DynamicSet[T, U]

  case class DynamicSetValue[T, U](property: Quoted[T] => Quoted[U], value: Quoted[U]) extends DynamicSet[T, U]
  case class DynamicSetEmpty[T, U]() extends DynamicSet[T, U]

  def set[T, U](property: Quoted[T] => Quoted[U], value: Quoted[U]): DynamicSet[T, U] =
    DynamicSetValue(property, value)

  def setValue[T, U](property: Quoted[T] => Quoted[U], value: U)(implicit enc: Encoder[U]): DynamicSet[T, U] =
    set[T, U](property, spliceLift(value))

  def setOpt[T, U](property: Quoted[T] => Quoted[U], value: Option[U])(implicit enc: Encoder[U]): DynamicSet[T, U] =
    value match {
      case Some(v) => setValue(property, v)
      case None    => DynamicSetEmpty()
    }

  def set[T, U](property: String, value: Quoted[U]): DynamicSet[T, U] =
    set((f: Quoted[T]) => splice(Property(f.ast, property)), value)

  def setValue[T, U](property: String, value: U)(implicit enc: Encoder[U]): DynamicSet[T, U] =
    set(property, spliceLift(value))

  def dynamicQuerySchema[T](entity: String, columns: DynamicAlias[T]*): DynamicEntityQuery[T] = {
    val aliases =
      columns.map { alias =>

        @tailrec def path(ast: Ast, acc: List[String] = Nil): List[String] =
          ast match {
            case Property(a, name) =>
              path(a, name :: acc)
            case _ =>
              acc
          }

        PropertyAlias(path(alias.property(splice[T](Ident("v"))).ast), alias.name)
      }
    DynamicEntityQuery(splice[EntityQuery[T]](Entity.Opinionated(entity, aliases.toList, Fixed)))
  }

  private[this] val nextIdentId = new DynamicVariable(0)

  private[this] def withFreshIdent[R](f: Ident => R): R = {
    val idx = nextIdentId.value
    nextIdentId.withValue(idx + 1) {
      f(Ident(s"v$idx"))
    }
  }

  private def dyn[T](ast: Ast): DynamicQuery[T] =
    DynamicQuery[T](splice[Query[T]](ast))

  private def splice[T](a: Ast) =
    new Quoted[T] {
      override def ast = a
    }

  protected def spliceLift[O](o: O)(implicit enc: Encoder[O]) =
    splice[O](ScalarValueLift("o", o, enc))

  object DynamicQuery {
    def apply[T](p: Quoted[Query[T]]) =
      new DynamicQuery[T] {
        override def q = p
      }
  }

  sealed trait DynamicQuery[+T] {

    protected[getquill] def q: Quoted[Query[T]]

    protected[this] def transform[U, V, R](f: Quoted[U] => Quoted[V], t: (Ast, Ident, Ast) => Ast, r: Ast => R = dyn _) =
      withFreshIdent { v =>
        r(t(q.ast, v, f(splice(v)).ast))
      }

    protected[this] def transformOpt[O, R, D <: DynamicQuery[T]](opt: Option[O], f: (Quoted[T], Quoted[O]) => Quoted[R], t: (Quoted[T] => Quoted[R]) => D, thiz: D)(implicit enc: Encoder[O]) =
      opt match {
        case Some(o) =>
          t(v => f(v, spliceLift(o)))
        case None =>
          thiz
      }

    def map[R](f: Quoted[T] => Quoted[R]): DynamicQuery[R] =
      transform(f, Map)

    def flatMap[R](f: Quoted[T] => Quoted[Query[R]]): DynamicQuery[R] =
      transform(f, FlatMap)

    def filter(f: Quoted[T] => Quoted[Boolean]): DynamicQuery[T] =
      transform(f, Filter)

    def withFilter(f: Quoted[T] => Quoted[Boolean]): DynamicQuery[T] =
      filter(f)

    def filterOpt[O](opt: Option[O])(f: (Quoted[T], Quoted[O]) => Quoted[Boolean])(implicit enc: Encoder[O]): DynamicQuery[T] =
      transformOpt(opt, f, filter, this)

    def filterIf(cond: Boolean)(f: Quoted[T] => Quoted[Boolean]): DynamicQuery[T] =
      if (cond) filter(f)
      else this

    def concatMap[R, U](f: Quoted[T] => Quoted[U])(implicit ev: U => Iterable[R]): DynamicQuery[R] =
      transform(f, ConcatMap)

    def sortBy[R](f: Quoted[T] => Quoted[R])(implicit ord: OrdDsl#Ord[R]): DynamicQuery[T] =
      transform(f, SortBy(_, _, _, ord.ord))

    def take(n: Quoted[Int]): DynamicQuery[T] =
      dyn(Take(q.ast, n.ast))

    def take(n: Int): DynamicQuery[T] =
      take(spliceLift(n))

    def takeOpt(opt: Option[Int]): DynamicQuery[T] =
      opt match {
        case Some(o) => take(o)
        case None    => this
      }

    def drop(n: Quoted[Int]): DynamicQuery[T] =
      dyn(Drop(q.ast, n.ast))

    def drop(n: Int): DynamicQuery[T] =
      drop(spliceLift(n))

    def dropOpt(opt: Option[Int]): DynamicQuery[T] =
      opt match {
        case Some(o) => drop(o)
        case None    => this
      }

    def ++[U >: T](q2: Quoted[Query[U]]): DynamicQuery[U] =
      dyn(UnionAll(q.ast, q2.ast))

    def unionAll[U >: T](q2: Quoted[Query[U]]): DynamicQuery[U] =
      dyn(UnionAll(q.ast, q2.ast))

    def union[U >: T](q2: Quoted[Query[U]]): DynamicQuery[U] =
      dyn(Union(q.ast, q2.ast))

    def groupBy[R](f: Quoted[T] => Quoted[R]): DynamicQuery[(R, Query[T])] =
      transform(f, GroupBy)

    private def aggregate(op: AggregationOperator) =
      splice(Aggregation(op, q.ast))

    def min[U >: T]: Quoted[Option[T]] =
      aggregate(AggregationOperator.min)

    def max[U >: T]: Quoted[Option[T]] =
      aggregate(AggregationOperator.max)

    def avg[U >: T](implicit n: Numeric[U]): Quoted[Option[T]] =
      aggregate(AggregationOperator.avg)

    def sum[U >: T](implicit n: Numeric[U]): Quoted[Option[T]] =
      aggregate(AggregationOperator.sum)

    def size: Quoted[Long] =
      aggregate(AggregationOperator.size)

    def join[A >: T, B](q2: Quoted[Query[B]]): DynamicJoinQuery[A, B, (A, B)] =
      DynamicJoinQuery(InnerJoin, q, q2)

    def leftJoin[A >: T, B](q2: Quoted[Query[B]]): DynamicJoinQuery[A, B, (A, Option[B])] =
      DynamicJoinQuery(LeftJoin, q, q2)

    def rightJoin[A >: T, B](q2: Quoted[Query[B]]): DynamicJoinQuery[A, B, (Option[A], B)] =
      DynamicJoinQuery(RightJoin, q, q2)

    def fullJoin[A >: T, B](q2: Quoted[Query[B]]): DynamicJoinQuery[A, B, (Option[A], Option[B])] =
      DynamicJoinQuery(FullJoin, q, q2)

    private[this] def flatJoin[R](tpe: JoinType, on: Quoted[T] => Quoted[Boolean]): DynamicQuery[R] =
      withFreshIdent { v =>
        dyn(FlatJoin(tpe, q.ast, v, on(splice(v)).ast))
      }

    def join[A >: T](on: Quoted[A] => Quoted[Boolean]): DynamicQuery[A] =
      flatJoin(InnerJoin, on)

    def leftJoin[A >: T](on: Quoted[A] => Quoted[Boolean]): DynamicQuery[Option[A]] =
      flatJoin(LeftJoin, on)

    def rightJoin[A >: T](on: Quoted[A] => Quoted[Boolean]): DynamicQuery[Option[A]] =
      flatJoin(RightJoin, on)

    def nonEmpty: Quoted[Boolean] =
      splice(UnaryOperation(SetOperator.nonEmpty, q.ast))

    def isEmpty: Quoted[Boolean] =
      splice(UnaryOperation(SetOperator.isEmpty, q.ast))

    def contains[B >: T](value: B)(implicit enc: Encoder[B]): Quoted[Boolean] =
      contains(spliceLift(value))

    def contains[B >: T](value: Quoted[B]): Quoted[Boolean] =
      splice(BinaryOperation(q.ast, SetOperator.contains, value.ast))

    def distinct: DynamicQuery[T] =
      dyn(Distinct(q.ast))

    def nested: DynamicQuery[T] =
      dyn(Nested(q.ast))

    override def toString = q.toString
  }

  case class DynamicJoinQuery[A, B, R](tpe: JoinType, q1: Quoted[Query[A]], q2: Quoted[Query[B]]) {
    def on(f: (Quoted[A], Quoted[B]) => Quoted[Boolean]): DynamicQuery[R] = {
      withFreshIdent { iA =>
        withFreshIdent { iB =>
          dyn(Join(tpe, q1.ast, q2.ast, iA, iB, f(splice(iA), splice(iB)).ast))
        }
      }
    }
  }

  case class DynamicEntityQuery[T](q: Quoted[EntityQuery[T]])
    extends DynamicQuery[T] {

    private[this] def dyn[R](ast: Ast) =
      DynamicEntityQuery(splice[EntityQuery[R]](ast))

    override def filter(f: Quoted[T] => Quoted[Boolean]): DynamicEntityQuery[T] =
      transform(f, Filter, dyn)

    override def withFilter(f: Quoted[T] => Quoted[Boolean]): DynamicEntityQuery[T] =
      filter(f)

    override def filterOpt[O](opt: Option[O])(f: (Quoted[T], Quoted[O]) => Quoted[Boolean])(implicit enc: Encoder[O]): DynamicEntityQuery[T] =
      transformOpt(opt, f, filter, this)

    override def map[R](f: Quoted[T] => Quoted[R]): DynamicEntityQuery[R] =
      transform(f, Map, dyn)

    def insertValue(value: T): DynamicInsert[T] = macro DynamicQueryDslMacro.insertValue

    type DynamicAssignment[U] = ((Quoted[T] => Quoted[U]), U)

    private[this] def assignemnts[S](l: List[DynamicSet[S, _]]): List[Assignment] =
      l.collect {
        case s: DynamicSetValue[_, _] =>
          val v = Ident("v")
          Assignment(v, s.property(splice(v)).ast, s.value.ast)
      }

    def insert(l: DynamicSet[T, _]*): DynamicInsert[T] =
      DynamicInsert(splice(Insert(DynamicEntityQuery.this.q.ast, assignemnts(l.toList))))

    def updateValue(value: T): DynamicUpdate[T] = macro DynamicQueryDslMacro.updateValue

    def update(sets: DynamicSet[T, _]*): DynamicUpdate[T] =
      DynamicUpdate(splice[Update[T]](Update(DynamicEntityQuery.this.q.ast, assignemnts(sets.toList))))

    def delete: DynamicDelete[T] =
      DynamicDelete(splice[Delete[T]](Delete(DynamicEntityQuery.this.q.ast)))
  }

  object DynamicAction {
    def apply[A <: Action[_]](p: Quoted[A]) =
      new DynamicAction[A] {
        override val q = p
      }
  }

  sealed trait DynamicAction[A <: Action[_]] {
    protected[getquill] def q: Quoted[A]

    override def toString = q.toString
  }

  object DynamicInsert {
    def apply[E](p: Quoted[Insert[E]]) =
      new DynamicInsert[E] {
        override val q = p
      }
  }

  trait DynamicInsert[E] extends DynamicAction[Insert[E]] {

    private[this] def dyn[R](ast: Ast) =
      DynamicInsert[R](splice(ast))

    def returning[R](f: Quoted[E] => Quoted[R]): DynamicActionReturning[E, R] =
      withFreshIdent { v =>
        DynamicActionReturning(splice(Returning(q.ast, v, f(splice(v)).ast)))
      }

    def returningGenerated[R](f: Quoted[E] => Quoted[R]): DynamicActionReturning[E, R] =
      withFreshIdent { v =>
        DynamicActionReturning(splice(ReturningGenerated(q.ast, v, f(splice(v)).ast)))
      }

    def onConflictIgnore: DynamicInsert[E] =
      dyn(OnConflict(DynamicInsert.this.q.ast, OnConflict.NoTarget, OnConflict.Ignore))

    def onConflictIgnore(targets: (Quoted[E] => Quoted[Any])*): DynamicInsert[E] = {
      val v = splice[E](Ident("v"))
      val properties =
        targets.toList.map { f =>
          f(v).ast match {
            case p: Property => p
            case p =>
              fail(s"Invalid ignore column: $p")
          }
        }
      dyn(OnConflict(DynamicInsert.this.q.ast, OnConflict.Properties(properties), OnConflict.Ignore))
    }
  }

  case class DynamicActionReturning[E, Output](q: Quoted[ActionReturning[E, Output]]) extends DynamicAction[ActionReturning[E, Output]]
  case class DynamicUpdate[E](q: Quoted[Update[E]]) extends DynamicAction[Update[E]]
  case class DynamicDelete[E](q: Quoted[Delete[E]]) extends DynamicAction[Delete[E]]
}
