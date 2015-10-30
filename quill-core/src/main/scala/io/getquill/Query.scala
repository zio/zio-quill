package io.getquill

sealed trait Query[+T] {

  def map[R](f: T => R): Query[R]
  def flatMap[R](f: T => Query[R]): Query[R]
  def withFilter(f: T => Boolean): Query[T]
  def filter(f: T => Boolean): Query[T]
  def sortBy[R](f: T => R)(implicit ord: Ordering[R]): SortedQuery[T]

  def take(n: Int): Query[T]
  def drop(n: Int): Query[T]

  def ++[U >: T](q: Query[U]): Query[U]
  def unionAll[U >: T](q: Query[U]): Query[U]
  def union[U >: T](q: Query[U]): Query[U]

  def groupBy[R](f: T => R): Query[(R, Query[T])]

  def min[U >: T](implicit n: Numeric[U]): Option[T]
  def max[U >: T](implicit n: Numeric[U]): Option[T]
  def avg[U >: T](implicit n: Numeric[U]): Option[T]
  def sum[U >: T](implicit n: Numeric[U]): T
  def size: Long

  def leftJoin[A >: T, B](q: Query[B]): OuterJoinQuery[A, B, (A, Option[B])]
  def rightJoin[A >: T, B](q: Query[B]): OuterJoinQuery[A, B, (Option[A], B)]
  def fullJoin[A >: T, B](q: Query[B]): OuterJoinQuery[A, B, (Option[A], Option[B])]

  def nonEmpty: Boolean
  def isEmpty: Boolean
}

sealed trait OuterJoinQuery[A, B, R] extends Query[R] {
  def on(f: (A, B) => Boolean): Query[R]
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

  def apply(alias: String, propertyAlias: (T => (Any, String))*): Query[T] with Insertable[T] with Updatable[T] with Deletable[T]

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
