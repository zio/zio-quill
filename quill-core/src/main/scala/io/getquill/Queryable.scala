package io.getquill

trait Queryable[+T] {

  def map[R](f: T => R): Queryable[R] = ???
  def flatMap[R](f: T => Queryable[R]): Queryable[R] = ???
  def withFilter(f: T => Any): Queryable[T] = ???
  def filter(f: T => Any): Queryable[T] = ???
}
