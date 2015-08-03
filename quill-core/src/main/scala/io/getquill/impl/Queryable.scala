package io.getquill.impl

import scala.reflect.ClassTag

sealed trait Queryable[+T] {

  def map[R](f: T => R): Queryable[R] = NonQuotedException()

  def flatMap[R](f: T => Queryable[R]): Queryable[R] = NonQuotedException()

  def withFilter(f: T => Any): Queryable[T] = NonQuotedException()

  def filter(f: T => Any): Queryable[T] = NonQuotedException()
}

sealed trait TableQueryable[+T] extends Queryable[T] {

  override def map[R](f: T => R): TableQueryable[R] = NonQuotedException()
  override def withFilter(f: T => Any): TableQueryable[T] = NonQuotedException()
  override def filter(f: T => Any): TableQueryable[T] = NonQuotedException()
}
