package io.getquill

sealed trait Queryable[+T] {

  def map[R](f: T => R): Queryable[R]
  def flatMap[R](f: T => Queryable[R]): Queryable[R]
  def withFilter(f: T => Boolean): Queryable[T]
  def filter(f: T => Boolean): Queryable[T]
  def sortBy[R](f: T => R)(implicit ord: Ordering[R]): SortedQueryable[T]
  def nonEmpty: Boolean
  def isEmpty: Boolean
}

sealed trait SortedQueryable[+T] extends Queryable[T] {

  def reverse: SortedQueryable[T]

  def map[R](f: T => R): SortedQueryable[R]
  def flatMap[R](f: T => Queryable[R]): SortedQueryable[R]
  def withFilter(f: T => Boolean): SortedQueryable[T]
  def filter(f: T => Boolean): SortedQueryable[T]
}

sealed trait EntityQueryable[+T] extends Queryable[T] {

  def insert(f: (T => (Any, Any))*): Insertable[T]
  def update(f: (T => (Any, Any))*): Updatable[T]
  def delete: Deletable[T]

  override def withFilter(f: T => Boolean): EntityQueryable[T]
  override def filter(f: T => Boolean): EntityQueryable[T]
}

sealed trait Actionable[+T]
sealed trait Insertable[+T] extends Actionable[T]
sealed trait Updatable[+T] extends Actionable[T]
sealed trait Deletable[+T] extends Actionable[T]
