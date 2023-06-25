package io.getquill

import io.getquill.ast.Ast
import io.getquill.quotation.NonQuotedException

import scala.annotation.compileTimeOnly

/**
 * A Quill-Action-Concept centrally defines Quill Query, Insert, Update, Delete,
 * etc... actions. This ZIO-inspired construct makes it easier to reason about
 * Quoted actions (particularly in Dotty) in a type-full way.
 */
sealed trait QAC[ModificationEntity, +OutputEntity]

sealed trait Query[+T] extends QAC[Nothing, T] {

  def map[R](f: T => R): Query[R] = NonQuotedException()

  def flatMap[R](f: T => Query[R]): Query[R] = NonQuotedException()

  def concatMap[R, U](f: T => U)(implicit ev: U => Iterable[R]): Query[R] = NonQuotedException()

  def withFilter(f: T => Boolean): Query[T] = NonQuotedException()
  def filter(f: T => Boolean): Query[T]     = NonQuotedException()

  def sortBy[R](f: T => R)(implicit ord: Ord[R]): Query[T] = NonQuotedException()

  def take(n: Int): Query[T] = NonQuotedException()
  def drop(n: Int): Query[T] = NonQuotedException()

  def ++[U >: T](q: Query[U]): Query[U]       = NonQuotedException()
  def unionAll[U >: T](q: Query[U]): Query[U] = NonQuotedException()
  def union[U >: T](q: Query[U]): Query[U]    = NonQuotedException()

  def groupBy[R](f: T => R): Query[(R, Query[T])]           = NonQuotedException()
  def groupByMap[G, R](by: T => G)(mapTo: T => R): Query[R] = NonQuotedException()

  def value[U >: T]: Option[T]                                = NonQuotedException()
  def min[U >: T]: Option[T]                                  = NonQuotedException()
  def max[U >: T]: Option[T]                                  = NonQuotedException()
  def avg[U >: T](implicit n: Numeric[U]): Option[BigDecimal] = NonQuotedException()
  def sum[U >: T](implicit n: Numeric[U]): Option[T]          = NonQuotedException()
  def size: Long                                              = NonQuotedException()

  def join[A >: T, B](q: Query[B]): JoinQuery[A, B, (A, B)]                     = NonQuotedException()
  def leftJoin[A >: T, B](q: Query[B]): JoinQuery[A, B, (A, Option[B])]         = NonQuotedException()
  def rightJoin[A >: T, B](q: Query[B]): JoinQuery[A, B, (Option[A], B)]        = NonQuotedException()
  def fullJoin[A >: T, B](q: Query[B]): JoinQuery[A, B, (Option[A], Option[B])] = NonQuotedException()

  def join[A >: T](on: A => Boolean): Query[A]              = NonQuotedException()
  def leftJoin[A >: T](on: A => Boolean): Query[Option[A]]  = NonQuotedException()
  def rightJoin[A >: T](on: A => Boolean): Query[Option[A]] = NonQuotedException()

  def nonEmpty: Boolean                   = NonQuotedException()
  def isEmpty: Boolean                    = NonQuotedException()
  def contains[B >: T](value: B): Boolean = NonQuotedException()

  def distinct: Query[T]                 = NonQuotedException()
  def distinctOn[R](f: T => R): Query[T] = NonQuotedException()

  def nested: Query[T] = NonQuotedException()

  /**
   * @param unquote
   *   is used for conversion of `Quoted[A]` to A` with `unquote`
   * @return
   */
  def foreach[A <: QAC[_, _] with Action[_], B](f: T => B)(implicit unquote: B => A): BatchAction[A] =
    NonQuotedException()
}

sealed trait JoinQuery[A, B, R] extends Query[R] {
  def on(f: (A, B) => Boolean): Query[R] = NonQuotedException()
}

trait EntityQueryModel[T] extends Query[T] {

  override def withFilter(f: T => Boolean): EntityQueryModel[T] = NonQuotedException()
  override def filter(f: T => Boolean): EntityQueryModel[T]     = NonQuotedException()
  override def map[R](f: T => R): EntityQueryModel[R]           = NonQuotedException()

  // Note: This class is to be shared with Dotty and the parameter `value` needs to be inline.
  //       however, regular values cannot be overridden with inline ones so we cannot define
  //       insert[T] and update[T] on this level.
  // def insert(value: T): Insert[T] = NonQuotedException()
  // def update(value: T): Update[T] = NonQuotedException()

  def insert(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Insert[T] = NonQuotedException()

  def update(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Update[T] = NonQuotedException()

  def delete: Delete[T] = NonQuotedException()
}

sealed trait Action[E] extends QAC[E, Any]

sealed trait Insert[E] extends QAC[E, Nothing] with Action[E] {
  @compileTimeOnly(NonQuotedException.message)
  def returning[R](f: E => R): ActionReturning[E, R] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def returningGenerated[R](f: E => R): ActionReturning[E, R] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def returningMany[R](f: E => R): ActionReturning[E, List[R]] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def onConflictIgnore: Insert[E] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def onConflictIgnore(target: E => Any, targets: (E => Any)*): Insert[E] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def onConflictUpdate(assign: ((E, E) => (Any, Any)), assigns: ((E, E) => (Any, Any))*): Insert[E] =
    NonQuotedException()

  /**
   * Generates an atomic INSERT or UPDATE (upsert) action if supported.
   *
   * @param targets
   *   \- conflict target
   * @param assigns
   *   \- update statement, declared as function: `(table, excluded) => (assign,
   *   result)` `table` - is used to extract column for update assignment and
   *   reference existing row `excluded` - aliases excluded table, e.g. row
   *   proposed for insertion. `assign` - left hand side of assignment. Should
   *   be accessed from `table` argument `result` - right hand side of
   *   assignment.
   *
   * Example usage:
   * {{{
   *   insert.onConflictUpdate(_.id)((t, e) => t.col -> (e.col + t.col))
   * }}}
   * If insert statement violates conflict target then the column `col` of row
   * will be updated with sum of existing value and proposed `col` in insert.
   */
  @compileTimeOnly(NonQuotedException.message)
  def onConflictUpdate(target: E => Any, targets: (E => Any)*)(
    assign: ((E, E) => (Any, Any)),
    assigns: ((E, E) => (Any, Any))*
  ): Insert[E] = NonQuotedException()
}

sealed trait ActionReturning[E, +Output] extends QAC[E, Output] with Action[E]

sealed trait Update[E] extends QAC[E, Nothing] with Action[E] {
  @compileTimeOnly(NonQuotedException.message)
  def returning[R](f: E => R): ActionReturning[E, R] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def returningMany[R](f: E => R): ActionReturning[E, List[R]] = NonQuotedException()
}

sealed trait Delete[E] extends QAC[E, Nothing] with Action[E] {
  @compileTimeOnly(NonQuotedException.message)
  def returning[R](f: E => R): ActionReturning[E, R] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def returningMany[R](f: E => R): ActionReturning[E, List[R]] = NonQuotedException()
}

sealed trait BatchAction[+A <: QAC[_, _] with Action[_]]
