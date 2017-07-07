package io.getquill.dsl

import scala.language.experimental.macros

import io.getquill.quotation.NonQuotedException
import scala.annotation.compileTimeOnly

private[dsl] trait QueryDsl {
  dsl: CoreDsl =>

  def query[T]: EntityQuery[T] = macro QueryDslMacro.expandEntity[T]

  @compileTimeOnly(NonQuotedException.message)
  def querySchema[T](entity: String, columns: (T => (Any, String))*): EntityQuery[T] = NonQuotedException()

  sealed trait Query[+T] {

    def map[R](f: T => R): Query[R]
    def flatMap[R](f: T => Query[R]): Query[R]
    def withFilter(f: T => Boolean): Query[T]
    def filter(f: T => Boolean): Query[T]
    def sortBy[R](f: T => R)(implicit ord: OrdDsl#Ord[R]): Query[T]

    def take(n: Int): Query[T]
    def drop(n: Int): Query[T]

    def ++[U >: T](q: Query[U]): Query[U]
    def unionAll[U >: T](q: Query[U]): Query[U]
    def union[U >: T](q: Query[U]): Query[U]

    def groupBy[R](f: T => R): Query[(R, Query[T])]

    def min[U >: T]: Option[T]
    def max[U >: T]: Option[T]
    def avg[U >: T](implicit n: Numeric[U]): Option[BigDecimal]
    def sum[U >: T](implicit n: Numeric[U]): Option[T]
    def size: Long

    def join[A >: T, B](q: Query[B]): JoinQuery[A, B, (A, B)]
    def leftJoin[A >: T, B](q: Query[B]): JoinQuery[A, B, (A, Option[B])]
    def rightJoin[A >: T, B](q: Query[B]): JoinQuery[A, B, (Option[A], B)]
    def fullJoin[A >: T, B](q: Query[B]): JoinQuery[A, B, (Option[A], Option[B])]

    def join[A >: T](on: A => Boolean): Query[A]
    def leftJoin[A >: T](on: A => Boolean): Query[Option[A]]
    def rightJoin[A >: T](on: A => Boolean): Query[Option[A]]

    def nonEmpty: Boolean
    def isEmpty: Boolean
    def contains[B >: T](value: B): Boolean

    def distinct: Query[T]

    def nested: Query[T]

    /**
     *
     * @param unquote is used for conversion of [[Quoted[A]]] to [[A]] with [[unquote]]
     * @return
     */
    def foreach[A <: Action[_], B](f: T => B)(implicit unquote: B => A): BatchAction[A]
  }

  sealed trait JoinQuery[A, B, R] extends Query[R] {
    def on(f: (A, B) => Boolean): Query[R]
  }

  sealed trait EntityQuery[T]
    extends Query[T] {

    override def withFilter(f: T => Boolean): EntityQuery[T]
    override def filter(f: T => Boolean): EntityQuery[T]
    override def map[R](f: T => R): EntityQuery[R]

    def insert(value: T): Insert[T] = macro QueryDslMacro.expandInsert[T]
    def insert(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Insert[T]

    def update(value: T): Update[T] = macro QueryDslMacro.expandUpdate[T]
    def update(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Update[T]

    def delete: Delete[T]
  }

  sealed trait Action[E]

  sealed trait Insert[E] extends Action[E] {
    @compileTimeOnly(NonQuotedException.message)
    def returning[R](f: E => R): ActionReturning[E, R] = NonQuotedException()
  }

  sealed trait ActionReturning[E, Output] extends Action[E]
  sealed trait Update[E] extends Action[E]
  sealed trait Delete[E] extends Action[E]

  sealed trait BatchAction[+A <: Action[_]]
}
