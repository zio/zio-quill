package io.getquill.context.zio

import com.github.jasync.sql.db.{ ConcreteConnection, QueryResult }
import com.github.jasync.sql.db.pool.{ ConnectionPool => KConnectionPool }
import zio.{ Has, RIO, Task, TaskManaged, ZIO, ZLayer, ZManaged, Tag }
import scala.jdk.CollectionConverters._

trait ZIOJAsyncConnection {
  protected def takeConnection: TaskManaged[ConcreteConnection]

  private[zio] final def transaction[A](action: RIO[Has[ZIOJAsyncConnection], A]): ZIO[Has[ZIOJAsyncConnection], Throwable, A] = {
    //Taken from ConcreteConnectionBase.kt to avoid usage of pool.inTransaction
    takeConnection.use(conn =>
      (ZIOJAsyncConnection.sendQuery(conn, "BEGIN") *>
        action.updateService[ZIOJAsyncConnection](_ => ZIOJAsyncConnection.make(conn))).tapBoth(
          _ => ZIOJAsyncConnection.sendQuery(conn, "ROLLBACK"),
          _ => ZIOJAsyncConnection.sendQuery(conn, "COMMIT")
        ))

  }

  private[zio] final def sendQuery(query: String): Task[QueryResult] =
    takeConnection.use(conn => ZIO.fromCompletableFuture(conn.sendQuery(query)))

  private[zio] final def sendPreparedStatement(sql: String, params: Seq[Any]): Task[QueryResult] =
    takeConnection.use(conn => ZIO.fromCompletableFuture(
      conn.sendPreparedStatement(sql, params.asJava)
    ))

}

object ZIOJAsyncConnection {

  def sendQuery(query: String): ZIO[Has[ZIOJAsyncConnection], Throwable, QueryResult] =
    ZIO.accessM[Has[ZIOJAsyncConnection]](_.get.sendQuery(query))

  def sendPreparedStatement(sql: String, params: Seq[Any]): ZIO[Has[ZIOJAsyncConnection], Throwable, QueryResult] =
    ZIO.accessM[Has[ZIOJAsyncConnection]](_.get.sendPreparedStatement(sql, params))

  private def sendQuery[C <: ConcreteConnection](connection: C, query: String): Task[QueryResult] =
    ZIO.fromCompletableFuture(connection.sendQuery(query))

  def make[C <: ConcreteConnection](pool: KConnectionPool[C]): ZIOJAsyncConnection = new ZIOJAsyncConnection {

    override protected def takeConnection: TaskManaged[ConcreteConnection] =
      ZManaged.make(ZIO.fromCompletableFuture(pool.take()))(conn => ZIO.fromCompletableFuture(pool.giveBack(conn)).orDie.unit)

  }

  def make[C <: ConcreteConnection](connection: C): ZIOJAsyncConnection = new ZIOJAsyncConnection {

    override protected def takeConnection: TaskManaged[ConcreteConnection] =
      ZManaged.succeed(connection)

  }

  def live[C <: ConcreteConnection: Tag]: ZLayer[Has[JAsyncContextConfig[C]], Throwable, Has[ZIOJAsyncConnection]] =
    ZManaged.accessManaged[Has[JAsyncContextConfig[C]]](env =>
      ZManaged.make(
        ZIO.effect(
          new KConnectionPool[C](
            env.get.connectionFactory(env.get.connectionPoolConfiguration.getConnectionConfiguration),
            env.get.connectionPoolConfiguration
          )
        )
      )(pool => ZIO.fromCompletableFuture(pool.disconnect()).orDie)).map(ZIOJAsyncConnection.make[C]).toLayer

}
