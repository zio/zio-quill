package io.getquill.context

import com.typesafe.config.Config
import io.getquill.JdbcContextConfig
import _root_.zio.{ Has, Task, ZIO, ZLayer, ZManaged }
import _root_.zio.stream.ZStream
import io.getquill.util.LoadConfig
import izumi.reflect.Tag

import java.io.Closeable
import java.sql.{ Connection, SQLException }
import javax.sql.DataSource

object ZioJdbc {
  import _root_.zio.blocking._

  /** Describes a single HOCON Jdbc Config block */
  case class Prefix(name: String)

  type QIO[T] = ZIO[BlockingConnection, SQLException, T]
  type QStream[T] = ZStream[BlockingConnection, SQLException, T]
  type BlockingConnection = Has[Connection] with Blocking
  type BlockingDataSource = Has[DataSource with Closeable] with Blocking

  object QIO {
    def apply[T](t: => T): QIO[T] = ZIO.effect(t).refineToOrDie[SQLException]
  }

  object Layers {
    val dataSourceToConnection: ZLayer[BlockingDataSource, SQLException, BlockingConnection] = {
      val managed =
        for {
          fromBlocking <- ZManaged.environment[Has[DataSource with Closeable] with Blocking]
          from = fromBlocking.get[DataSource with Closeable]
          blocking = fromBlocking.get[Blocking.Service]
          r <- ZManaged.fromAutoCloseable(ZIO.effect(from.getConnection).refineToOrDie[SQLException]: ZIO[Any, SQLException, Connection])
        } yield Has(r) ++ Has(blocking)
      ZLayer.fromManagedMany(managed)
    }

    def dataSourceFromConfig(config: => Config): ZLayer[Blocking, Throwable, BlockingDataSource] =
      dataSourceFromJdbcConfig(JdbcContextConfig(config))

    def dataSourceFromPrefix(prefix: Prefix): ZLayer[Blocking, Throwable, BlockingDataSource] =
      dataSourceFromJdbcConfig(JdbcContextConfig(LoadConfig(prefix.name)))

    def dataSourceFromPrefix(prefix: String): ZLayer[Blocking, Throwable, BlockingDataSource] =
      dataSourceFromJdbcConfig(JdbcContextConfig(LoadConfig(prefix)))

    def dataSourceFromJdbcConfig(jdbcContextConfig: => JdbcContextConfig): ZLayer[Blocking, Throwable, BlockingDataSource] =
      ZLayer.fromManagedMany(
        for {
          block <- ZManaged.environment[Blocking]
          conf <- ZManaged.fromEffect(Task(jdbcContextConfig))
          ds <- ZManaged.fromAutoCloseable(Task(conf.dataSource: DataSource with Closeable))
        } yield (Has(ds) ++ block)
      )
  }

  import Layers._

  implicit class ZioQuillThrowableExt[T](qzio: ZIO[BlockingConnection, Throwable, T]) {
    def justSqlEx = qzio.refineToOrDie[SQLException]
  }

  def dependOnDs[T](qzio: ZIO[BlockingConnection, Throwable, T]) =
    qzio.justSqlEx.provideLayer(dataSourceToConnection)
  def provideConnection[T](qzio: ZIO[BlockingConnection, Throwable, T])(conn: Connection): ZIO[Blocking, SQLException, T] =
    provideOne(conn)(qzio.justSqlEx)
  def provideConnectionFrom[T](qzio: ZIO[BlockingConnection, Throwable, T])(ds: DataSource with Closeable): ZIO[Blocking, SQLException, T] =
    provideOne(ds)(ZioJdbc.dependOnDs(qzio.justSqlEx))

  implicit class DataSourceCloseableExt(ds: DataSource with Closeable) {
    def withDefaultBlocking: BlockingDataSource = Has(ds) ++ Has(Blocking.Service.live)
  }

  implicit class QuillZioExt[T](qzio: ZIO[BlockingConnection, Throwable, T]) {
    /**
     * Allows the user to specify `Has[DataSource]` instead of `Has[Connection]` for a Quill ZIO value i.e.
     * Converts:<br>
     *   `ZIO[BlockingConnection, Throwable, T]` to `ZIO[BlockingDataSource, Throwable, T]` a.k.a.<br>
     *   `ZIO[Has[Connection] with Blocking, Throwable, T]` to `ZIO[Has[DataSource] with Blocking, Throwable, T]` a.k.a.<br>
     */
    def dependOnDs(): ZIO[BlockingDataSource, SQLException, T] = ZioJdbc.dependOnDs(qzio)

    /**
     * Allows the user to specify JDBC `DataSource` instead of `BlockingConnection` for a Quill ZIO value i.e.
     * Provides a DataSource object which internally brackets `dataSource.getConnection` and `connection.close()`.
     * This effectively converts:<br>
     *   `ZIO[BlockingConnection, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     *   `ZIO[Has[Connection] with Blocking, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     */
    def provideConnectionFrom(ds: DataSource with Closeable): ZIO[Blocking, SQLException, T] =
      ZioJdbc.provideConnectionFrom(qzio)(ds)

    /**
     * Allows the user to specify JDBC `Connection` instead of `BlockingConnection` for a Quill ZIO value i.e.
     * Provides a Connection object which converts:<br>
     *   `ZIO[BlockingConnection, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     *   `ZIO[Has[Connection] with Blocking, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     */
    def provideConnection(conn: Connection): ZIO[Blocking, SQLException, T] =
      ZioJdbc.provideConnection(qzio)(conn)
  }

  private[getquill] def provideOne[P: Tag, T, E: Tag, Rest <: Has[_]: Tag](provision: P)(qzio: ZIO[Has[P] with Rest, E, T]): ZIO[Rest, E, T] =
    for {
      rest <- ZIO.environment[Rest]
      env = Has(provision) ++ rest
      result <- qzio.provide(env)
    } yield result
}
