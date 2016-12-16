package io.getquill.sources.async

sealed trait AsyncIO[A] {
  def flatMap[B](f: A => AsyncIO[B]): AsyncIO[B] = FlatMapCmd(this, f)
  def map[B](f: A => B) = Map(this, f)
}

case class AsyncIOValue[R](v: R) extends AsyncIO[R]

case class PrepearedStmtCmd[R](
  sql:    String,
  params: List[Any]
) extends AsyncIO[R]

case class FlatMapCmd[A, B](
  a: AsyncIO[A],
  f: A => AsyncIO[B]
) extends AsyncIO[B]

case class Map[A, B](a: AsyncIO[A], f: A => B) extends AsyncIO[B]

case class TransCmd[A](
  action: AsyncIO[A]
) extends AsyncIO[A]

object AsyncIO {
  def pure[V](v: V): AsyncIO[V] = AsyncIOValue(v)
}
