package io.getquill.context.zio

import com.github.jasync.sql.db.{ ConcreteConnection, QueryResult }
import com.github.jasync.sql.db.pool.{ ConnectionPool => KConnectionPool }
import zio.{ RIO, Task, TaskManaged, ZIO, ZLayer, ZManaged, Tag }
import scala.jdk.CollectionConverters._

trait ZioJAsyncConnection {
  protected def takeConnection: TaskManaged[ConcreteConnection]

  private[zio] final def transaction[A](action: RIO[ZioJAsyncConnection, A]): ZIO[ZioJAsyncConnection, Throwable, A] = {
    //Taken from ConcreteConnectionBase.kt to avoid usage of pool.inTransaction
    takeConnection.use(conn =>
      (ZioJAsyncConnection.sendQuery(conn, "BEGIN") *>
        action.updateService[ZioJAsyncConnection](_ => ZioJAsyncConnection.make(conn))).tapBoth(
          _ => ZioJAsyncConnection.sendQuery(conn, "ROLLBACK"),
          _ => ZioJAsyncConnection.sendQuery(conn, "COMMIT")
        ))

  }

  private[zio] final def sendQuery(query: String): Task[QueryResult] =
    takeConnection.use(conn => ZIO.fromCompletableFuture(conn.sendQuery(query)))

  private[zio] final def sendPreparedStatement(sql: String, params: Seq[Any]): Task[QueryResult] =
    takeConnection.use(conn => ZIO.fromCompletableFuture(
      conn.sendPreparedStatement(sql, params.asJava)
    ))

}

object ZioJAsyncConnection {

  def sendQuery(query: String): ZIO[ZioJAsyncConnection, Throwable, QueryResult] =
    ZIO.environmentWithZIO[ZioJAsyncConnection](_.get.sendQuery(query))

  def sendPreparedStatement(sql: String, params: Seq[Any]): ZIO[ZioJAsyncConnection, Throwable, QueryResult] =
    ZIO.environmentWithZIO[ZioJAsyncConnection](_.get.sendPreparedStatement(sql, params))

  private def sendQuery[C <: ConcreteConnection](connection: C, query: String): Task[QueryResult] =
    ZIO.fromCompletableFuture(connection.sendQuery(query))

  def make[C <: ConcreteConnection](pool: KConnectionPool[C]): ZioJAsyncConnection = new ZioJAsyncConnection {

    override protected def takeConnection: TaskManaged[ConcreteConnection] =
      ZManaged.acquireReleaseWith(ZIO.fromCompletableFuture(pool.take()))(conn => ZIO.fromCompletableFuture(pool.giveBack(conn)).orDie.unit)

  }

  def make[C <: ConcreteConnection](connection: C): ZioJAsyncConnection = new ZioJAsyncConnection {

    override protected def takeConnection: TaskManaged[ConcreteConnection] =
      ZManaged.succeed(connection)

  }

  def live[C <: ConcreteConnection: Tag]: ZLayer[JAsyncContextConfig[C], Throwable, ZioJAsyncConnection] =
    ZManaged.environmentWithManaged[JAsyncContextConfig[C]](env =>
      ZManaged.acquireReleaseWith(
        ZIO.attempt(
          new KConnectionPool[C](
            env.get.connectionFactory(env.get.connectionPoolConfiguration.getConnectionConfiguration),
            env.get.connectionPoolConfiguration
          )
        )
      )(pool => ZIO.fromCompletableFuture(pool.disconnect()).orDie)).map(ZioJAsyncConnection.make[C]).toLayer

}
