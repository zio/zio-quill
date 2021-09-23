package io.getquill.context.zio

import com.github.jasync.sql.db.{ ConcreteConnection, QueryResult }
import com.github.jasync.sql.db.pool.{ ConnectionPool => KConnectionPool }
import zio.{ Has, RIO, Task, TaskManaged, ZIO, ZLayer, ZManaged, Tag }
import scala.jdk.CollectionConverters._

object ZConnection {

  trait Service {
    protected def takeConnection: TaskManaged[ConcreteConnection]

    private[zio] final def transaction[A](action: RIO[Has[Service], A]): ZIO[Has[Service], Throwable, A] = {
      //Taken from ConcreteConnectionBase.kt to avoid usage of pool.inTransaction
      takeConnection.use(conn =>
        (ZConnection.sendQuery(conn, "BEGIN") *>
          action.updateService[Service](_ => Service.live(conn))).tapBoth(
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

    final def execute[R <: Has[Service], A](action: RIO[R, A]): RIO[R, A] =
      action.provideSomeLayer[R](ZLayer.succeed(this))
  }

  def sendQuery(query: String): ZIO[Has[Service], Throwable, QueryResult] =
    ZIO.accessM[Has[Service]](_.get.sendQuery(query))

  def sendPreparedStatement(sql: String, params: Seq[Any]): ZIO[Has[Service], Throwable, QueryResult] =
    ZIO.accessM[Has[Service]](_.get.sendPreparedStatement(sql, params))

  private def sendQuery[C <: ConcreteConnection](connection: C, query: String): Task[QueryResult] =
    ZIO.fromCompletableFuture(connection.sendQuery(query))

  object Service {
    def live[C <: ConcreteConnection](pool: KConnectionPool[C]): Service = new Service {

      override protected def takeConnection: TaskManaged[ConcreteConnection] =
        ZManaged.make(ZIO.fromCompletableFuture(pool.take()))(conn => ZIO.fromCompletableFuture(pool.giveBack(conn)).orDie.unit)

    }

    def live[C <: ConcreteConnection](connection: C): Service = new Service {

      override protected def takeConnection: TaskManaged[ConcreteConnection] =
        ZManaged.succeed(connection)

    }
  }

  def live[C <: ConcreteConnection: Tag]: ZLayer[Has[JAsyncContextConfig[C]], Throwable, Has[Service]] =
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
