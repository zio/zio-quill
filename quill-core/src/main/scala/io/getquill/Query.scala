package io.getquill

sealed trait Query[+T] {

  def map[R](f: T => R): Query[R]
  def flatMap[R](f: T => Query[R]): Query[R]
  def withFilter(f: T => Boolean): Query[T]
  def filter(f: T => Boolean): Query[T]
  def sortBy[R](f: T => R)(implicit ord: Ordering[R]): SortedQuery[T]

  def take(n: Int): Query[T]
  def drop(n: Int): Query[T]

  def union[U >: T](q: Query[U]): Query[U]

  def nonEmpty: Boolean
  def isEmpty: Boolean
}

sealed trait SortedQuery[+T] extends Query[T] {

  def reverse: SortedQuery[T]

  def map[R](f: T => R): SortedQuery[R]
  def flatMap[R](f: T => Query[R]): SortedQuery[R]
  def withFilter(f: T => Boolean): SortedQuery[T]
  def filter(f: T => Boolean): SortedQuery[T]
}

sealed trait EntityQuery[+T]
    extends Query[T]
    with Insertable[T]
    with Updatable[T]
    with Deletable[T] {

  override def withFilter(f: T => Boolean): Query[T] with Updatable[T] with Deletable[T]
  override def filter(f: T => Boolean): Query[T] with Updatable[T] with Deletable[T]
}

sealed trait Action[+T]

sealed trait Insertable[+T] {
  def insert(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Action[T]
}
sealed trait Updatable[+T] {
  def update(f: (T => (Any, Any)), f2: (T => (Any, Any))*): Action[T]
}
sealed trait Deletable[+T] {
  def delete: Action[T]
}
