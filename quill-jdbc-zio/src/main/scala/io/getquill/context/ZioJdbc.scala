package io.getquill.context

import com.typesafe.config.Config
import io.getquill.JdbcContextConfig
import zio.{ Has, Task, ZIO, ZLayer, ZManaged }
import zio.stream.ZStream
import io.getquill.util.LoadConfig
import izumi.reflect.Tag

import java.io.Closeable
import java.sql.{ Connection, SQLException }
import javax.sql.DataSource

object ZioJdbc {
  import zio.blocking._

  /** Describes a single HOCON Jdbc Config block */
  case class Prefix(name: String)

  type QIO[T] = ZIO[QConnection, SQLException, T]
  type QStream[T] = ZStream[Has[Connection] with Blocking, SQLException, T]
  type QConnection = Has[Connection] with Blocking
  type QDataSource = Has[DataSource with Closeable] with Blocking

  object QIO {
    def apply[T](t: => T): QIO[T] = ZIO.effect(t).refineToOrDie[SQLException]
  }

  object QDataSource {
    val toConnection: ZLayer[QDataSource, SQLException, QConnection] = {
      val managed =
        for {
          fromBlocking <- ZManaged.environment[Has[DataSource with Closeable] with Blocking]
          from = fromBlocking.get[DataSource with Closeable]
          blocking = fromBlocking.get[Blocking.Service]
          r <- ZManaged.fromAutoCloseable(ZIO.effect(from.getConnection).refineToOrDie[SQLException]: ZIO[Any, SQLException, Connection])
        } yield Has(r) ++ Has(blocking)
      ZLayer.fromManagedMany(managed)
    }

    def fromConfig(config: => Config): ZLayer[Blocking, Throwable, QDataSource] =
      fromJdbcConfig(JdbcContextConfig(config))

    def fromPrefix(prefix: Prefix): ZLayer[Blocking, Throwable, QDataSource] =
      fromJdbcConfig(JdbcContextConfig(LoadConfig(prefix.name)))

    def fromPrefix(prefix: String): ZLayer[Blocking, Throwable, QDataSource] =
      fromJdbcConfig(JdbcContextConfig(LoadConfig(prefix)))

    def fromJdbcConfig(jdbcContextConfig: => JdbcContextConfig): ZLayer[Blocking, Throwable, QDataSource] =
      ZLayer.fromManagedMany(
        for {
          block <- ZManaged.environment[Blocking]
          conf <- ZManaged.fromEffect(Task(jdbcContextConfig))
          ds <- ZManaged.fromAutoCloseable(Task(conf.dataSource: DataSource with Closeable))
        } yield (Has(ds) ++ block)
      )
  }

  implicit class ZioQuillThrowableExt[T](qzio: ZIO[QConnection, Throwable, T]) {
    def justSqlEx = qzio.refineToOrDie[SQLException]
  }

  object QConnection {
    def fromDataSource: ZLayer[QDataSource, SQLException, QConnection] = QDataSource.toConnection
    def dependOnDataSource[T](qzio: ZIO[QConnection, Throwable, T]) =
      qzio.justSqlEx.provideLayer(QDataSource.toConnection)
    def provideConnection[T](qzio: ZIO[QConnection, Throwable, T])(conn: Connection): ZIO[Blocking, SQLException, T] =
      provideOne(conn)(qzio.justSqlEx)
    def provideConnectionFrom[T](qzio: ZIO[QConnection, Throwable, T])(ds: DataSource with Closeable): ZIO[Blocking, SQLException, T] =
      provideOne(ds)(QConnection.dependOnDataSource(qzio.justSqlEx))
  }

  implicit class DataSourceCloseableExt(ds: DataSource with Closeable) {
    def withDefaultBlocking: QDataSource = Has(ds) ++ Has(Blocking.Service.live)
  }

  implicit class QuillZioExt[T](qzio: ZIO[QConnection, Throwable, T]) {
    /**
     * Allows the user to specify `Has[DataSource]` instead of `Has[Connection]` for a Quill ZIO value i.e.
     * Converts:<br>
     *   `ZIO[QConnection, Throwable, T]` to `ZIO[QDataSource, Throwable, T]` a.k.a.<br>
     *   `ZIO[Has[Connection] with Blocking, Throwable, T]` to `ZIO[Has[DataSource] with Blocking, Throwable, T]` a.k.a.<br>
     */
    def dependOnDataSource(): ZIO[QDataSource, SQLException, T] = QConnection.dependOnDataSource(qzio)

    /**
     * Allows the user to specify JDBC `DataSource` instead of `QConnection` for a Quill ZIO value i.e.
     * Provides a DataSource object which internally brackets `dataSource.getConnection` and `connection.close()`.
     * This effectively converts:<br>
     *   `ZIO[QConnection, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     *   `ZIO[Has[Connection] with Blocking, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     */
    def provideConnectionFrom(ds: DataSource with Closeable): ZIO[Blocking, SQLException, T] =
      QConnection.provideConnectionFrom(qzio)(ds)

    /**
     * Allows the user to specify JDBC `Connection` instead of `QConnection` for a Quill ZIO value i.e.
     * Provides a Connection object which converts:<br>
     *   `ZIO[QConnection, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     *   `ZIO[Has[Connection] with Blocking, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     */
    def provideConnection(conn: Connection): ZIO[Blocking, SQLException, T] =
      QConnection.provideConnection(qzio)(conn)
  }

  private[getquill] def provideOne[P: Tag, T, E: Tag, Rest <: Has[_]: Tag](provision: P)(qzio: ZIO[Has[P] with Rest, E, T]): ZIO[Rest, E, T] =
    for {
      rest <- ZIO.environment[Rest]
      env = Has(provision) ++ rest
      result <- qzio.provide(env)
    } yield result
}
