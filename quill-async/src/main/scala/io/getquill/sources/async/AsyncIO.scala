package io.getquill.sources.async

import com.github.mauricio.async.db.QueryResult
import scala.concurrent.Future

sealed trait AsyncIO[A] {
  def flatMap[B](f: A => AsyncIO[B]): AsyncIO[B] = FlatMapCmd(this, f)
  def map[B](f: A => B) = MapCmd(this, f)
  def unsafePerformIO(implicit pool: AsyncPool): Future[A] = pool.execute(this)
  def unsafePerformTrans(implicit pool: AsyncPool) = TransCmd(this).unsafePerformIO
}

case class AsyncIOValue[R](v: R) extends AsyncIO[R]

case class SqlCmd[R](sql: String, extractor: QueryResult => R) extends AsyncIO[R]

case class QueryCmd[R](
  sql:       String,
  params:    List[Any],
  extractor: QueryResult => R
) extends AsyncIO[R]

case class ExecuteCmd[R](
  sql:       String,
  params:    List[Any],
  extractor: QueryResult => R
) extends AsyncIO[R]

case class FlatMapCmd[A, B](
  a: AsyncIO[A],
  f: A => AsyncIO[B]
) extends AsyncIO[B]

case class MapCmd[A, B](a: AsyncIO[A], f: A => B) extends AsyncIO[B]

case class TransCmd[A](
  action: AsyncIO[A]
) extends AsyncIO[A]

object AsyncIO {
  def pure[V](v: V): AsyncIO[V] = AsyncIOValue(v)
}
