package io.getquill

sealed trait Queryable[+T] {

  def map[R](f: T => R): Queryable[R]
  def flatMap[R](f: T => Queryable[R]): Queryable[R]
  def withFilter(f: T => Boolean): Queryable[T]
  def filter(f: T => Boolean): Queryable[T]
  def sortBy[R](f: T => R)(implicit ord: Ordering[R]): SortedQueryable[T]

  def take(n: Int): Queryable[T]
  def drop(n: Int): Queryable[T]

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

sealed trait EntityQueryable[+T]
    extends Queryable[T]
    with Insertable[T]
    with Updatable[T]
    with Deletable[T] {

  override def withFilter(f: T => Boolean): Queryable[T] with Updatable[T] with Deletable[T]
  override def filter(f: T => Boolean): Queryable[T] with Updatable[T] with Deletable[T]
}

sealed trait Actionable[+T]

sealed trait Insertable[+T] {
  def insert(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Actionable[T]
}
sealed trait Updatable[+T] {
  def update(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Actionable[T]
}
sealed trait Deletable[+T] {
  def delete: Actionable[T]
}
