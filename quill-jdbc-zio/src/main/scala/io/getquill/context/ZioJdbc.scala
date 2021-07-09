package io.getquill.context

import com.typesafe.config.Config
import io.getquill.JdbcContextConfig
import io.getquill.context.qzio.ImplicitSyntax.Implicit
import zio.{ Has, IO, Task, ZIO, ZLayer, ZManaged }
import zio.stream.ZStream
import io.getquill.util.{ ContextLogger, LoadConfig }

import java.io.Closeable
import java.sql.{ Connection, SQLException }
import javax.sql.DataSource

object ZioJdbc {
  type QIO[T] = ZIO[Has[Connection], SQLException, T]
  type QStream[T] = ZStream[Has[Connection], SQLException, T]

  object QIO {
    def apply[T](t: => T): QIO[T] = ZIO.effect(t).refineToOrDie[SQLException]
  }

  object DaoLayer {
    def live: ZLayer[Has[DataSource with Closeable], SQLException, Has[Connection]] = layer

    private[getquill] val layer = {
      val managed =
        for {
          from <- ZManaged.environment[Has[DataSource with Closeable]]
          r <- managedBestEffort(ZIO.effect(from.get.getConnection).refineToOrDie[SQLException]: ZIO[Any, SQLException, Connection])
        } yield Has(r)
      ZLayer.fromManagedMany(managed)
    }

    def fromDataSource(ds: => DataSource with Closeable): ZLayer[Any, Throwable, Has[Connection]] =
      ZLayer.fromEffect(Task(ds)) >>> live

    def fromConfig(config: => Config): ZLayer[Any, Throwable, Has[Connection]] =
      fromJdbcConfig(JdbcContextConfig(config))

    def fromPrefix(prefix: String): ZLayer[Any, Throwable, Has[Connection]] =
      fromJdbcConfig(JdbcContextConfig(LoadConfig(prefix)))

    def fromJdbcConfig(jdbcContextConfig: => JdbcContextConfig): ZLayer[Any, Throwable, Has[Connection]] =
      ZLayer.fromManagedMany(
        for {
          conf <- ZManaged.fromEffect(Task(jdbcContextConfig))
          ds <- managedBestEffort(Task(conf.dataSource: DataSource with Closeable))
        } yield Has(ds)
      ) >>> live
  }

  object QDataSource {
    object Managed {
      @deprecated(message = "Not needed anymore. Use: ZioJdbc.managedBestEffort(Task(ds)).map(Has(_))", "3.8.0")
      def fromDataSource(ds: => DataSource with Closeable) =
        managedBestEffort(Task(ds)).map(Has(_))
    }

    @deprecated("Use DaoLayer.live instead", "3.8.0")
    val toConnection: ZLayer[Has[DataSource with Closeable], SQLException, Has[Connection]] =
      DaoLayer.live

    @deprecated("Use ZLayer.fromEffect(Task(ds)) instead", "3.8.0")
    def fromDataSource(ds: => DataSource with Closeable): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      ZLayer.fromEffect(Task(ds))

    def fromConfig(config: => Config): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      fromJdbcConfig(JdbcContextConfig(config))

    def fromPrefix(prefix: String): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      fromJdbcConfig(JdbcContextConfig(LoadConfig(prefix)))

    def fromJdbcConfig(jdbcContextConfig: => JdbcContextConfig): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      ZLayer.fromManagedMany(
        for {
          conf <- ZManaged.fromEffect(Task(jdbcContextConfig))
          ds <- managedBestEffort(Task(conf.dataSource: DataSource with Closeable))
        } yield Has(ds)
      )
  }

  implicit class ZioQuillThrowableExt[T](qzio: ZIO[Has[Connection], Throwable, T]) {
    @deprecated("Use .refineToOrDie[SQLException]", "3.8.0")
    def justSqlEx = qzio.refineToOrDie[SQLException]
  }

  object QConnection {
    @deprecated("Use DaoLayer.live. If you need just SQLException add .refineToOrDie[SQLException]", "3.8.0")
    def fromDataSource: ZLayer[Has[DataSource with Closeable], SQLException, Has[Connection]] = DaoLayer.live

    @deprecated("Use qzio.asDao.", "3.8.0")
    def dependOnDataSource[T](qzio: ZIO[Has[Connection], Throwable, T]) =
      qzio.asDao

    @deprecated("Use qzio.provide(Has(conn)). If you need just SQLException add .refineToOrDie[SQLException]", "3.8.0")
    def provideConnection[T](qzio: ZIO[Has[Connection], Throwable, T])(conn: Connection): ZIO[Any, SQLException, T] =
      qzio.provide(Has(conn)).refineToOrDie[SQLException]

    @deprecated("Use qzio.asDao.provide(Has(ds)). If you need just SQLException add .refineToOrDie[SQLException]", "3.8.0")
    def provideConnectionFrom[T](qzio: ZIO[Has[Connection], Throwable, T])(ds: DataSource with Closeable): ZIO[Any, SQLException, T] =
      qzio.asDao.provide(Has(ds)).refineToOrDie[SQLException]
  }

  implicit class QuillZioExt[T](qzio: ZIO[Has[Connection], Throwable, T]) {
    import io.getquill.context.qzio.ImplicitSyntax._

    def asDao: ZIO[Has[DataSource with Closeable], SQLException, T] =
      qzio.provideLayer(DaoLayer.live).refineToOrDie[SQLException]

    def implicitDao(implicit implicitEnv: Implicit[Has[DataSource with Closeable]]): IO[SQLException, T] =
      qzio.asDao.implicitly

    @deprecated("Use qzio.asDao.", "3.8.0")
    def dependOnDataSource(): ZIO[Has[DataSource with Closeable], SQLException, T] =
      qzio.asDao.refineToOrDie[SQLException]

    @deprecated("Use qzio.asDao.provide(Has(ds)). If you need just SQLException add .refineToOrDie[SQLException]", "3.8.0")
    def provideConnectionFrom(ds: DataSource with Closeable): ZIO[Nothing, SQLException, T] =
      qzio.asDao.provide(Has(ds)).refineToOrDie[SQLException]

    @deprecated("Use qzio.provide(Has(conn)). If you need just SQLException add .refineToOrDie[SQLException]", "3.8.0")
    def provideConnection(conn: Connection): ZIO[Any, SQLException, T] =
      qzio.provide(Has(conn)).refineToOrDie[SQLException]
  }

  /**
   * This is the same as `ZManaged.fromAutoCloseable` but if the `.close()` fails it will log `"close() of resource failed"`
   * and continue instead of immediately throwing an error in the ZIO die-channel. That is because for JDBC purposes,
   * a failure on the connection close is usually a recoverable failure. In the cases where it happens it occurs
   * as the byproduct of a bad state (e.g. failing to close a transaction before closing the connection or failing to
   * release a stale connection) which will eventually cause other operations (i.e. future reads/writes) to fail
   * that have not occurred yet.
   */
  def managedBestEffort[R, E, A <: AutoCloseable](effect: ZIO[R, E, A]) =
    ZManaged.make(effect)(resource =>
      ZIO.effect(resource.close()).tapError(e => ZIO.effect(logger.underlying.error(s"close() of resource failed", e)).ignore).ignore)

  private[getquill] val logger = ContextLogger(ZioJdbc.getClass)
}

