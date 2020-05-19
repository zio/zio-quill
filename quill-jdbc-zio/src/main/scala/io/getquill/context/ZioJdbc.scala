package io.getquill.context

import com.typesafe.config.Config
import io.getquill.JdbcContextConfig
import _root_.zio.{ Has, Task, ZIO, ZLayer, ZManaged }
import io.getquill.util.LoadConfig
import izumi.reflect.Tag

import java.io.Closeable
import java.sql.Connection
import javax.sql.DataSource

object ZioJdbc {
  import _root_.zio.blocking._

  /** Describes a single HOCON Jdbc Config block */
  case class Prefix(name: String)

  type BlockingConnection = Has[Connection] with Blocking
  type BlockingDataSource = Has[DataSource with Closeable] with Blocking

  object Layers {
    val dataSourceToConnection: ZLayer[BlockingDataSource, Throwable, BlockingConnection] =
      mapAutoCloseableGeneric[DataSource with Closeable, Connection]((from: DataSource) => from.getConnection)

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

  def dependOnDs[T](qzio: ZIO[BlockingConnection, Throwable, T]): ZIO[BlockingDataSource, Throwable, T] =
    qzio.provideLayer(dataSourceToConnection)
  def provideConnection[T](qzio: ZIO[BlockingConnection, Throwable, T])(conn: Connection): ZIO[Blocking, Throwable, T] =
    provideOne(conn)(qzio)
  def provideConnectionFrom[T](qzio: ZIO[BlockingConnection, Throwable, T])(ds: DataSource with Closeable): ZIO[Blocking, Throwable, T] =
    provideOne(ds)(ZioJdbc.dependOnDs(qzio))

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
    def dependOnDs(): ZIO[BlockingDataSource, Throwable, T] = ZioJdbc.dependOnDs(qzio)

    /**
     * Allows the user to specify JDBC `DataSource` instead of `BlockingConnection` for a Quill ZIO value i.e.
     * Provides a DataSource object which internally brackets `dataSource.getConnection` and `connection.close()`.
     * This effectively converts:<br>
     *   `ZIO[BlockingConnection, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     *   `ZIO[Has[Connection] with Blocking, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     */
    def provideConnectionFrom(ds: DataSource with Closeable): ZIO[Blocking, Throwable, T] =
      ZioJdbc.provideConnectionFrom(qzio)(ds)

    /**
     * Allows the user to specify JDBC `Connection` instead of `BlockingConnection` for a Quill ZIO value i.e.
     * Provides a Connection object which converts:<br>
     *   `ZIO[BlockingConnection, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     *   `ZIO[Has[Connection] with Blocking, Throwable, T]` to `ZIO[Blocking, Throwable, T]` a.k.a.<br>
     */
    def provideConnection(conn: Connection): ZIO[Blocking, Throwable, T] =
      ZioJdbc.provideConnection(qzio)(conn)
  }

  private[getquill] def provideOne[P: Tag, T, E: Tag, Rest <: Has[_]: Tag](provision: P)(qzio: ZIO[Has[P] with Rest, E, T]): ZIO[Rest, E, T] =
    for {
      rest <- ZIO.environment[Rest]
      env = Has(provision) ++ rest
      result <- qzio.provide(env)
    } yield result

  private[getquill] def mapAutoCloseableGeneric[From: Tag, To <: AutoCloseable: Tag](mapping: From => To): ZLayer[Has[From] with Blocking, Throwable, Has[To] with Blocking] = {
    val managed =
      for {
        fromBlocking <- ZManaged.environment[Has[From] with Blocking]
        from = fromBlocking.get[From]
        blocking = fromBlocking.get[Blocking.Service]
        r <- ZManaged.fromAutoCloseable(Task(mapping(from)))
      } yield Has(r) ++ Has(blocking)
    ZLayer.fromManagedMany(managed)
  }
}
