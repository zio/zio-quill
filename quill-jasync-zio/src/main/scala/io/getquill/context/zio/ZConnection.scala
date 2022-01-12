package io.getquill.context.zio

import com.github.jasync.sql.db.{ ConcreteConnection, QueryResult }
import com.github.jasync.sql.db.pool.{ ConnectionPool => KConnectionPool }
import zio.{ Has, RIO, Task, TaskManaged, ZIO, ZLayer, ZManaged, Tag }
import scala.jdk.CollectionConverters._

trait ZConnection {
  protected def takeConnection: TaskManaged[ConcreteConnection]

  private[zio] final def transaction[A](action: RIO[Has[ZConnection], A]): ZIO[Has[ZConnection], Throwable, A] = {
    //Taken from ConcreteConnectionBase.kt to avoid usage of pool.inTransaction
    takeConnection.use(conn =>
      (ZConnection.sendQuery(conn, "BEGIN") *>
        action.updateService[ZConnection](_ => ZConnection.make(conn))).tapBoth(
        _ => ZConnection.sendQuery(conn, "ROLLBACK"),
        _ => ZConnection.sendQuery(conn, "COMMIT")
      ))

  }

  private[zio] final def sendQuery(query: String): Task[QueryResult] =
    takeConnection.use(conn => ZIO.fromCompletableFuture(conn.sendQuery(query)))

  private[zio] final def sendPreparedStatement(sql: String, params: Seq[Any]): Task[QueryResult] =
    takeConnection.use(conn => ZIO.fromCompletableFuture(
      conn.sendPreparedStatement(sql, params.asJava)
    ))

  final def execute[R <: Has[ZConnection], A](action: RIO[R, A]): RIO[R, A] =
    action.provideSomeLayer[R](ZLayer.succeed(this))
}

object ZConnection {

  def sendQuery(query: String): ZIO[Has[ZConnection], Throwable, QueryResult] =
    ZIO.accessM[Has[ZConnection]](_.get.sendQuery(query))

  def sendPreparedStatement(sql: String, params: Seq[Any]): ZIO[Has[ZConnection], Throwable, QueryResult] =
    ZIO.accessM[Has[ZConnection]](_.get.sendPreparedStatement(sql, params))

  private def sendQuery[C <: ConcreteConnection](connection: C, query: String): Task[QueryResult] =
    ZIO.fromCompletableFuture(connection.sendQuery(query))

  def make[C <: ConcreteConnection](pool: KConnectionPool[C]): ZConnection = new ZConnection {

    override protected def takeConnection: TaskManaged[ConcreteConnection] =
      ZManaged.make(ZIO.fromCompletableFuture(pool.take()))(conn => ZIO.fromCompletableFuture(pool.giveBack(conn)).orDie.unit)

  }

  def make[C <: ConcreteConnection](connection: C): ZConnection = new ZConnection {

    override protected def takeConnection: TaskManaged[ConcreteConnection] =
      ZManaged.succeed(connection)

  }

  def live[C <: ConcreteConnection: Tag]: ZLayer[Has[JAsyncContextConfig[C]], Throwable, Has[ZConnection]] =
    ZManaged.accessManaged[Has[JAsyncContextConfig[C]]](env =>
      ZManaged.make(
        ZIO.effect(
          new KConnectionPool[C](
            env.get.connectionFactory(env.get.connectionPoolConfiguration.getConnectionConfiguration),
            env.get.connectionPoolConfiguration
          )
        )
      )(pool => ZIO.fromCompletableFuture(pool.disconnect()).orDie)).map(Service.live[C]).toLayer

}
