package io.getquill.context

import com.typesafe.config.Config
import io.getquill.JdbcContextConfig
import io.getquill.util.{ ContextLogger, LoadConfig }
import io.getquill.jdbczio.Quill
import zio.{ Scope, ZEnvironment, ZIO, ZLayer }
import zio.stream.ZStream
import izumi.reflect.Tag

import java.io.Closeable
import java.sql.{ Connection, SQLException }
import javax.sql.DataSource

object ZioJdbc {
  type QIO[T] = ZIO[DataSource, SQLException, T]
  type QStream[T] = ZStream[DataSource, SQLException, T]

  type QCIO[T] = ZIO[Connection, SQLException, T]
  type QCStream[T] = ZStream[Connection, SQLException, T]

  object QIO {
    def apply[T](t: => T): QIO[T] = ZIO.attempt(t).refineToOrDie[SQLException]
  }

  object QCIO {
    def apply[T](t: => T): QCIO[T] = ZIO.attempt(t).refineToOrDie[SQLException]
  }

  object DataSourceLayer {
    @deprecated("Use Quill.Connection.acquireScoped instead", "3.3.0")
    val live: ZLayer[DataSource, SQLException, Connection] =
      ZLayer.scoped {
        for {
          blockingExecutor <- ZIO.blockingExecutor
          ds <- ZIO.service[DataSource]
          r <- ZioJdbc.scopedBestEffort(ZIO.attempt(ds.getConnection)).refineToOrDie[SQLException].onExecutor(blockingExecutor)
        } yield r
      }

    @deprecated("Use ZLayer.succeed(dataSource:DataSource) instead", "3.3.0")
    def fromDataSource(ds: => DataSource): ZLayer[Any, Throwable, DataSource] =
      ZLayer.fromZIO(ZIO.attempt(ds))

    @deprecated("Use Quill.DataSource.fromConfig instead", "3.3.0")
    def fromConfig(config: => Config): ZLayer[Any, Throwable, DataSource] =
      fromConfigClosable(config)

    @deprecated("Use Quill.DataSource.fromPrefix instead", "3.3.0")
    def fromPrefix(prefix: String): ZLayer[Any, Throwable, DataSource] =
      fromPrefixClosable(prefix)

    @deprecated("Use Quill.DataSource.fromJdbcConfig instead", "3.3.0")
    def fromJdbcConfig(jdbcContextConfig: => JdbcContextConfig): ZLayer[Any, Throwable, DataSource] =
      fromJdbcConfigClosable(jdbcContextConfig)

    @deprecated("Use Quill.DataSource.fromConfigClosable instead", "3.3.0")
    def fromConfigClosable(config: => Config): ZLayer[Any, Throwable, DataSource with Closeable] =
      fromJdbcConfigClosable(JdbcContextConfig(config))

    @deprecated("Use Quill.DataSource.fromPrefixClosable instead", "3.3.0")
    def fromPrefixClosable(prefix: String): ZLayer[Any, Throwable, DataSource with Closeable] =
      fromJdbcConfigClosable(JdbcContextConfig(LoadConfig(prefix)))

    @deprecated("Use Quill.DataSource.fromJdbcConfigClosable instead", "3.3.0")
    def fromJdbcConfigClosable(jdbcContextConfig: => JdbcContextConfig): ZLayer[Any, Throwable, DataSource with Closeable] =
      ZLayer.scoped {
        for {
          conf <- ZIO.attempt(jdbcContextConfig)
          ds <- scopedBestEffort(ZIO.attempt(conf.dataSource))
        } yield ds
      }
  }

  implicit class QuillZioDataSourceExt[T](qzio: ZIO[DataSource, Throwable, T]) {
    import io.getquill.context.qzio.ImplicitSyntax._
    def implicitDS(implicit implicitEnv: Implicit[DataSource]): ZIO[Any, SQLException, T] =
      (for {
        q <- qzio.provideEnvironment(ZEnvironment(implicitEnv.env))
      } yield q).refineToOrDie[SQLException]
  }

  implicit class QuillZioSomeDataSourceExt[T, R](qzio: ZIO[DataSource with R, Throwable, T])(implicit tag: Tag[R]) {
    import io.getquill.context.qzio.ImplicitSyntax._
    def implicitSomeDS(implicit implicitEnv: Implicit[DataSource]): ZIO[R, SQLException, T] =
      (for {
        r <- ZIO.environment[R]
        q <- qzio
          .provideSomeLayer[DataSource](ZLayer.succeedEnvironment(r))
          .provideEnvironment(ZEnvironment(implicitEnv.env))
      } yield q).refineToOrDie[SQLException]
  }

  implicit class QuillZioExtPlain[T](qzio: ZIO[Connection, Throwable, T]) {

    import io.getquill.context.qzio.ImplicitSyntax._

    def onDataSource: ZIO[DataSource, SQLException, T] =
      (for {
        q <- qzio.provideSomeLayer(Quill.Connection.acquireScoped)
      } yield q).refineToOrDie[SQLException]

    def implicitDS(implicit implicitEnv: Implicit[DataSource]): ZIO[Any, SQLException, T] =
      (for {
        q <- qzio
          .provideSomeLayer(Quill.Connection.acquireScoped)
          .provideEnvironment(ZEnvironment(implicitEnv.env))
      } yield q).refineToOrDie[SQLException]
  }

  implicit class QuillZioExt[T, R](qzio: ZIO[Connection with R, Throwable, T])(implicit tag: Tag[R]) {
    /**
     * Change `Connection` of a QIO to `DataSource with Closeable` by providing a `Quill.Connection.acquireScoped` instance
     * which will grab a connection from the data-source, perform the QIO operation, and the immediately release the connection.
     * This is used for data-sources that have pooled connections e.g. Hikari.
     * {{{
     *   def ds: DataSource with Closeable = ...
     *   run(query[Person]).onDataSource.provide(Has(ds))
     * }}}
     */
    def onSomeDataSource: ZIO[DataSource with R, SQLException, T] =
      (for {
        r <- ZIO.environment[R]
        q <- qzio
          .provideSomeLayer[Connection](ZLayer.succeedEnvironment(r))
          .provideSomeLayer(Quill.Connection.acquireScoped)
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
  private[getquill] def scopedBestEffort[R, E, A <: AutoCloseable](effect: ZIO[R, E, A]): ZIO[R with Scope, E, A] =
    ZIO.acquireRelease(effect)(resource =>
      ZIO.attemptBlocking(resource.close()).tapError(e => ZIO.attempt(logger.underlying.error(s"close() of resource failed", e)).ignore).ignore)

  private[getquill] val streamBlocker: ZStream[Any, Nothing, Any] =
    ZStream.scoped {
      for {
        executor <- ZIO.executor
        blockingExecutor <- ZIO.blockingExecutor
        _ <- ZIO.acquireRelease(ZIO.shift(blockingExecutor))(_ => ZIO.shift(executor))
      } yield ()
    }

  private[getquill] val logger = ContextLogger(ZioJdbc.getClass)
}

