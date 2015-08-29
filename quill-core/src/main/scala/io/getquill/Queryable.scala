package io.getquill

import io.getquill.quotation.NonQuotedException

sealed trait Queryable[+T] {
  def nonEmpty: Boolean
  def isEmpty: Boolean
}

sealed trait ComposableQueryable[+T] extends Queryable[T] {

  def map[R](f: T => R): ComposableQueryable[R]
  def flatMap[R](f: T => Queryable[R]): ComposableQueryable[R]
  def withFilter(f: T => Any): ComposableQueryable[T]
  def filter(f: T => Any): ComposableQueryable[T]
  def sortBy(f: T => Any): SortedQueryable[T]
}

sealed trait SortedQueryable[+T] extends Queryable[T] {
  def map[R](f: T => R): Queryable[R]
}

sealed trait EntityQueryable[+T] extends ComposableQueryable[T] {

  def insert(f: (T => (Any, Any))*): Insertable[T]
  def update(f: (T => (Any, Any))*): Updatable[T]
  def delete: Deletable[T]

  override def withFilter(f: T => Any): EntityQueryable[T]
  override def filter(f: T => Any): EntityQueryable[T]
}

sealed trait Actionable[+T]
sealed trait Insertable[+T] extends Actionable[T]
sealed trait Updatable[+T] extends Actionable[T]
sealed trait Deletable[+T] extends Actionable[T]
