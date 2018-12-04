package io.getquill.context.finagle.mysql

import com.twitter.concurrent.AsyncStream
import com.twitter.finagle.mysql
import com.twitter.util.{ Future, Time }
import java.util.concurrent.atomic.AtomicInteger
import com.twitter.finagle.mysql.Transactions
import com.twitter.finagle.mysql.Session
import com.twitter.finagle.mysql.Client

class OkTestClient extends mysql.Client with mysql.Transactions {
  val methodCount = new AtomicInteger

  val ok = mysql.OK(0, 0, 0, 0, "")

  override def query(sql: String): Future[mysql.Result] = {
    methodCount.incrementAndGet()
    Future(ok)
  }

  override def select[T](sql: String)(f: mysql.Row => T): Future[Seq[T]] = {
    methodCount.incrementAndGet()
    Future(Seq.empty)
  }
  override def prepare(sql: String): mysql.PreparedStatement = {
    methodCount.incrementAndGet()
    new mysql.PreparedStatement {
      override def apply(params: mysql.Parameter*): Future[mysql.Result] = Future(ok)
    }
  }
  override def cursor(sql: String): mysql.CursoredStatement = {
    methodCount.incrementAndGet()
    new mysql.CursoredStatement {
      override def apply[T](rowsPerFetch: Int, params: mysql.Parameter*)(f: mysql.Row => T): Future[mysql.CursorResult[T]] = Future {
        new mysql.CursorResult[T] {
          override def stream: AsyncStream[T] = AsyncStream.empty
          override def close(deadline: Time): Future[Unit] = Future.Unit
        }
      }
    }
  }

  override def ping(): Future[Unit] = {
    methodCount.incrementAndGet()
    Future.Unit
  }

  override def transaction[T](f: mysql.Client => Future[T]): Future[T] = {
    f(this)
  }
  override def transactionWithIsolation[T](isolationLevel: mysql.IsolationLevel)(f: mysql.Client => Future[T]): Future[T] = {
    f(this)
  }

  override def close(deadline: Time): Future[Unit] = Future.Unit

  def session[T](f: Client with Transactions with Session => Future[T]): Future[T] = {
    ???
  }
}
