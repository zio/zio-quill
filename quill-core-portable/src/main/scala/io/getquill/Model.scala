package io.getquill

import io.getquill.quotation.NonQuotedException

import scala.annotation.compileTimeOnly

sealed trait Query[+T] {

  def map[R](f: T => R): Query[R] = NonQuotedException()

  def flatMap[R](f: T => Query[R]): Query[R] = NonQuotedException()

  def concatMap[R, U](f: T => U)(implicit ev: U => Iterable[R]): Query[R] = NonQuotedException()

  def withFilter(f: T => Boolean): Query[T] = NonQuotedException()
  def filter(f: T => Boolean): Query[T] = NonQuotedException()

  def sortBy[R](f: T => R)(implicit ord: Ord[R]): Query[T] = NonQuotedException()

  def take(n: Int): Query[T] = NonQuotedException()
  def drop(n: Int): Query[T] = NonQuotedException()

  def ++[U >: T](q: Query[U]): Query[U] = NonQuotedException()
  def unionAll[U >: T](q: Query[U]): Query[U] = NonQuotedException()
  def union[U >: T](q: Query[U]): Query[U] = NonQuotedException()

  def groupBy[R](f: T => R): Query[(R, Query[T])] = NonQuotedException()

  def value[U >: T]: Option[T] = NonQuotedException()
  def min[U >: T]: Option[T] = NonQuotedException()
  def max[U >: T]: Option[T] = NonQuotedException()
  def avg[U >: T](implicit n: Numeric[U]): Option[BigDecimal] = NonQuotedException()
  def sum[U >: T](implicit n: Numeric[U]): Option[T] = NonQuotedException()
  def size: Long = NonQuotedException()

  def join[A >: T, B](q: Query[B]): JoinQuery[A, B, (A, B)] = NonQuotedException()
  def leftJoin[A >: T, B](q: Query[B]): JoinQuery[A, B, (A, Option[B])] = NonQuotedException()
  def rightJoin[A >: T, B](q: Query[B]): JoinQuery[A, B, (Option[A], B)] = NonQuotedException()
  def fullJoin[A >: T, B](q: Query[B]): JoinQuery[A, B, (Option[A], Option[B])] = NonQuotedException()

  def join[A >: T](on: A => Boolean): Query[A] = NonQuotedException()
  def leftJoin[A >: T](on: A => Boolean): Query[Option[A]] = NonQuotedException()
  def rightJoin[A >: T](on: A => Boolean): Query[Option[A]] = NonQuotedException()

  def nonEmpty: Boolean = NonQuotedException()
  def isEmpty: Boolean = NonQuotedException()
  def contains[B >: T](value: B): Boolean = NonQuotedException()

  def distinct: Query[T] = NonQuotedException()

  def nested: Query[T] = NonQuotedException()

  /**
   *
   * @param unquote is used for conversion of `Quoted[A]` to A` with `unquote`
   * @return
   */
  def foreach[A <: Action[_], B](f: T => B)(implicit unquote: B => A): BatchAction[A] = NonQuotedException()
}

sealed trait JoinQuery[A, B, R] extends Query[R] {
  def on(f: (A, B) => Boolean): Query[R] = NonQuotedException()
}

trait EntityQueryModel[T]
  extends Query[T] {

  override def withFilter(f: T => Boolean): EntityQueryModel[T] = NonQuotedException()
  override def filter(f: T => Boolean): EntityQueryModel[T] = NonQuotedException()
  override def map[R](f: T => R): EntityQueryModel[R] = NonQuotedException()

  def insert(value: T): Insert[T] = NonQuotedException()
  def insert(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Insert[T] = NonQuotedException()

  def update(value: T): Update[T] = NonQuotedException()
  def update(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Update[T] = NonQuotedException()

  def delete: Delete[T] = NonQuotedException()
}

sealed trait Action[E]

sealed trait Insert[E] extends Action[E] {
  @compileTimeOnly(NonQuotedException.message)
  def returning[R](f: E => R): ActionReturning[E, R] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def returningGenerated[R](f: E => R): ActionReturning[E, R] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def onConflictIgnore: Insert[E] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def onConflictIgnore(target: E => Any, targets: (E => Any)*): Insert[E] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def onConflictUpdate(assign: ((E, E) => (Any, Any)), assigns: ((E, E) => (Any, Any))*): Insert[E] = NonQuotedException()

  /**
   * Generates an atomic INSERT or UPDATE (upsert) action if supported.
   *
   * @param targets - conflict target
   * @param assigns - update statement, declared as function: `(table, excluded) => (assign, result)`
   *                `table` - is used to extract column for update assignment and reference existing row
   *                `excluded` - aliases excluded table, e.g. row proposed for insertion.
   *                `assign` - left hand side of assignment. Should be accessed from `table` argument
   *                `result` - right hand side of assignment.
   *
   * Example usage:
   * {{{
   *   insert.onConflictUpdate(_.id)((t, e) => t.col -> (e.col + t.col))
   * }}}
   * If insert statement violates conflict target then the column `col` of row will be updated with sum of
   * existing value and and proposed `col` in insert.
   */
  @compileTimeOnly(NonQuotedException.message)
  def onConflictUpdate(target: E => Any, targets: (E => Any)*)(assign: ((E, E) => (Any, Any)), assigns: ((E, E) => (Any, Any))*): Insert[E] = NonQuotedException()
}

sealed trait ActionReturning[E, Output] extends Action[E]

sealed trait Update[E] extends Action[E] {
  @compileTimeOnly(NonQuotedException.message)
  def returning[R](f: E => R): ActionReturning[E, R] = NonQuotedException()
}

sealed trait Delete[E] extends Action[E] {
  @compileTimeOnly(NonQuotedException.message)
  def returning[R](f: E => R): ActionReturning[E, R] = NonQuotedException()
}

sealed trait BatchAction[+A <: Action[_]]
