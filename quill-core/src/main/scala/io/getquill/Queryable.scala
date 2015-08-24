package io.getquill

import io.getquill.quotation.NonQuotedException

sealed trait Queryable[+T] {

  def map[R](f: T => R): Queryable[R] = NonQuotedException()

  def flatMap[R](f: T => Queryable[R]): Queryable[R] = NonQuotedException()

  def withFilter(f: T => Any): Queryable[T] = NonQuotedException()

  def filter(f: T => Any): Queryable[T] = NonQuotedException()

  def nonEmpty: Boolean = NonQuotedException()
  def isEmpty: Boolean = NonQuotedException()
}

sealed trait EntityQueryable[+T] extends Queryable[T] {

  def insert(f: (T => (Any, Any))*): Insertable[T] = NonQuotedException()
  def update(f: (T => (Any, Any))*): Updatable[T] = NonQuotedException()
  def delete: Deletable[T] = NonQuotedException()

  override def withFilter(f: T => Any): EntityQueryable[T] = NonQuotedException()
  override def filter(f: T => Any): EntityQueryable[T] = NonQuotedException()
}

sealed trait Actionable[+T]
class Insertable[+T] extends Actionable[T]
class Updatable[+T] extends Actionable[T]
class Deletable[+T] extends Actionable[T]
