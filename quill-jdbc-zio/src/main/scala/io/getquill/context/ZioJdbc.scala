package io.getquill.context

import com.typesafe.config.Config
import io.getquill.JdbcContextConfig
import zio.{ Has, Task, ZIO, ZLayer, ZManaged }
import zio.stream.ZStream
import io.getquill.util.{ ContextLogger, LoadConfig }
import izumi.reflect.Tag
import zio.blocking.Blocking

import java.io.Closeable
import java.sql.{ Connection, SQLException }
import javax.sql.DataSource

object ZioJdbc {
  type QIO[T] = ZIO[Has[DataSource], SQLException, T]
  type QStream[T] = ZStream[Has[DataSource], SQLException, T]

  type QCIO[T] = ZIO[Has[Connection], SQLException, T]
  type QCStream[T] = ZStream[Has[Connection], SQLException, T]

  object QIO {
    def apply[T](t: => T): QCIO[T] = ZIO.effect(t).refineToOrDie[SQLException]
  }

  implicit class DataSourceLayerExt(layer: ZLayer[Any, Throwable, Has[DataSource with Closeable]]) {
    def widen: ZLayer[Any, Throwable, Has[DataSource]] = layer.map(ds => Has(ds.get[DataSource with Closeable]: DataSource))
  }

  object DataSourceLayer {
    def live: ZLayer[Has[DataSource], SQLException, Has[Connection]] = layer

    private[getquill] val layer = {
      val managed =
        for {
          blockingExecutor <- ZIO.succeed(zio.blocking.Blocking.Service.live.blockingExecutor).toManaged_
          from <- ZManaged.environment[Has[DataSource]]
          r <- ZioJdbc.managedBestEffort(ZIO.effect(from.get.getConnection)).refineToOrDie[SQLException].lock(blockingExecutor)
        } yield Has(r)
      ZLayer.fromManagedMany(managed)
    }

    def fromDataSource(ds: => DataSource): ZLayer[Any, Throwable, Has[DataSource]] =
      ZLayer.fromEffect(Task(ds))

    def fromConfig(config: => Config): ZLayer[Any, Throwable, Has[DataSource]] =
      fromJdbcConfig(JdbcContextConfig(config))

    def fromPrefix(prefix: String): ZLayer[Any, Throwable, Has[DataSource]] =
      fromJdbcConfig(JdbcContextConfig(LoadConfig(prefix)))

    def fromJdbcConfig(jdbcContextConfig: => JdbcContextConfig): ZLayer[Any, Throwable, Has[DataSource]] =
      ZLayer.fromManagedMany(
        for {
          conf <- ZManaged.fromEffect(Task(jdbcContextConfig))
          ds <- managedBestEffort(Task(conf.dataSource: DataSource with Closeable))
        } yield Has(ds)
      )

    def fromConfigClosable(config: => Config): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      fromJdbcConfigClosable(JdbcContextConfig(config))

    def fromPrefixClosable(prefix: String): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      fromJdbcConfigClosable(JdbcContextConfig(LoadConfig(prefix)))

    def fromJdbcConfigClosable(jdbcContextConfig: => JdbcContextConfig): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      ZLayer.fromManagedMany(
        for {
          conf <- ZManaged.fromEffect(Task(jdbcContextConfig))
          ds <- managedBestEffort(Task(conf.dataSource: DataSource with Closeable))
        } yield Has(ds)
      )
  }

  object QDataSource {
    @deprecated("Use DataSourceLayer.fromConfig instead", "3.11.0")
    def fromConfig(config: => Config): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      fromJdbcConfig(JdbcContextConfig(config))

    @deprecated("Use DataSourceLayer.fromPrefix instead", "3.11.0")
    def fromPrefix(prefix: String): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      fromJdbcConfig(JdbcContextConfig(LoadConfig(prefix)))

    @deprecated("Use DataSourceLayer.fromJdbcConfig instead", "3.11.0")
    def fromJdbcConfig(jdbcContextConfig: => JdbcContextConfig): ZLayer[Any, Throwable, Has[DataSource with Closeable]] =
      ZLayer.fromManagedMany(
        for {
          conf <- ZManaged.fromEffect(Task(jdbcContextConfig))
          ds <- managedBestEffort(Task(conf.dataSource: DataSource with Closeable))
        } yield Has(ds)
      )
  }

  implicit class QuillZioDataSourceExt[T, R <: Has[_]](qzio: ZIO[Has[DataSource] with R, Throwable, T])(implicit tag: Tag[R]) {
    import io.getquill.context.qzio.ImplicitSyntax._

    def implicitDS(implicit implicitEnv: Implicit[Has[DataSource]]): ZIO[R, SQLException, T] =
      (for {
        r <- ZIO.environment[R]
        q <- qzio
          .provideSomeLayer[Has[DataSource]](ZLayer.succeedMany(r))
          .provide(implicitEnv.env)
      } yield q).refineToOrDie[SQLException]
  }

  implicit class QuillZioExtPlain[T](qzio: ZIO[Has[Connection], Throwable, T]) {

    import io.getquill.context.qzio.ImplicitSyntax._

    def onDataSource: ZIO[Has[DataSource], SQLException, T] =
      (for {
        q <- qzio.provideSomeLayer(DataSourceLayer.live)
      } yield q).refineToOrDie[SQLException]

    /** Shortcut for `onDataSource` */
    @deprecated("Use onDataSource", "3.10.0")
    def onDS: ZIO[Has[DataSource], SQLException, T] =
      this.onDataSource

    @deprecated(
      "Preferably use a ZioJdbcContext instead of a ZioJdbcUnderlyingContext when starting with a DataSource " +
        "(i.e. instead of a Connection). ZIO[Has[DataSource], SQLException, T] now has a .implicitDS method for similar functionality." +
        "If you need to convert from ZIO[Has[Connection], SQLException, T], to ZIO[Has[DataSource], SQLException, T] " +
        "use .provideLayer(DataSourceLayer.live)", "3.10.0"
    )
    def implicitDS(implicit implicitEnv: Implicit[Has[DataSource]]): ZIO[Any, SQLException, T] =
      (for {
        q <- qzio
          .provideSomeLayer(DataSourceLayer.live)
          .provide(implicitEnv.env)
      } yield q).refineToOrDie[SQLException]
  }

  implicit class QuillZioExt[T, R <: Has[_]](qzio: ZIO[Has[Connection] with R, Throwable, T])(implicit tag: Tag[R]) {
    /**
     * Change `Has[Connection]` of a QIO to `Has[DataSource with Closeable]` by providing a `DataSourceLayer.live` instance
     * which will grab a connection from the data-source, perform the QIO operation, and the immediately release the connection.
     * This is used for data-sources that have pooled connections e.g. Hikari.
     * {{{
     *   def ds: DataSource with Closeable = ...
     *   run(query[Person]).onDataSource.provide(Has(ds))
     * }}}
     */
    def onSomeDataSource: ZIO[Has[DataSource] with R, SQLException, T] =
      (for {
        r <- ZIO.environment[R]
        q <- qzio
          .provideSomeLayer[Has[Connection]](ZLayer.succeedMany(r))
          .provideSomeLayer(DataSourceLayer.live)
      } yield q).refineToOrDie[SQLException]
  }

  /**
   * This is the same as `ZManaged.fromAutoCloseable` but if the `.close()` fails it will log `"close() of resource failed"`
   * and continue instead of immediately throwing an error in the ZIO die-channel. That is because for JDBC purposes,
   * a failure on the connection close is usually a recoverable failure. In the cases where it happens it occurs
   * as the byproduct of a bad state (e.g. failing to close a transaction before closing the connection or failing to
   * release a stale connection) which will eventually cause other operations (i.e. future reads/writes) to fail
   * that have not occurred yet.
   */
  private[getquill] def managedBestEffort[R, E, A <: AutoCloseable](effect: ZIO[R, E, A]) =
    ZManaged.make(effect)(resource =>
      blockingEffect(resource.close()).tapError(e => ZIO.effect(logger.underlying.error(s"close() of resource failed", e)).ignore).ignore)

  private[getquill] val streamBlocker: ZStream[Any, Nothing, Any] =
    ZStream.managed(zio.blocking.blockingExecutor.toManaged_.flatMap { executor =>
      ZManaged.lock(executor)
    }).provideLayer(Blocking.live)

  private[getquill] def withBlocking[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    Blocking.Service.live.blocking(zio)

  private[getquill] def blockingEffect[A](value: => A): Task[A] =
    Blocking.Service.live.blocking(Task(value))

  private[getquill] val logger = ContextLogger(ZioJdbc.getClass)
}

