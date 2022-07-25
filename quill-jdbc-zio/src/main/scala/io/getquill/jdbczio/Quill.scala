package io.getquill.jdbczio

import com.typesafe.config.Config
import io.getquill._
import io.getquill.context.ZioJdbc
import io.getquill.context.ZioJdbc.scopedBestEffort
import io.getquill.context.jdbc._
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.LoadConfig
import zio.{ Tag, ZIO, ZLayer }

import java.io.Closeable
import java.sql.{ Connection, SQLException }
import javax.sql.DataSource

object Quill {
  case class PostgresService[+N <: NamingStrategy](naming: N, override val ds: DataSource)
    extends Quill[PostgresDialect, N] with PostgresJdbcTypes[N] {
    val dsDelegate = new PostgresZioJdbcContext[N](naming)
  }

  object PostgresService {
    def apply[N <: NamingStrategy: Tag](naming: N): LayerConstructor[ZLayer[javax.sql.DataSource, Nothing, PostgresService[N]]] =
      new LayerConstructor(ZLayer.fromFunction((ds: javax.sql.DataSource) => new PostgresService[N](naming, ds)))
  }

  case class SqlServerService[+N <: NamingStrategy](naming: N, override val ds: DataSource)
    extends Quill[SQLServerDialect, N] with SqlServerJdbcTypes[N] {
    val dsDelegate = new SqlServerZioJdbcContext[N](naming)
  }
  object SqlServerService {
    def apply[N <: NamingStrategy: Tag](naming: N): LayerConstructor[ZLayer[javax.sql.DataSource, Nothing, SqlServerService[N]]] =
      new LayerConstructor(ZLayer.fromFunction((ds: javax.sql.DataSource) => new SqlServerService[N](naming, ds)))
  }

  case class H2Service[+N <: NamingStrategy](naming: N, override val ds: DataSource)
    extends Quill[H2Dialect, N] with H2JdbcTypes[N] {
    val dsDelegate = new H2ZioJdbcContext[N](naming)
  }
  object H2Service {
    def apply[N <: NamingStrategy: Tag](naming: N): LayerConstructor[ZLayer[javax.sql.DataSource, Nothing, H2Service[N]]] =
      new LayerConstructor(ZLayer.fromFunction((ds: javax.sql.DataSource) => new H2Service[N](naming, ds)))
  }

  case class MysqlService[+N <: NamingStrategy](naming: N, override val ds: DataSource)
    extends Quill[MySQLDialect, N] with MysqlJdbcTypes[N] {
    val dsDelegate = new MysqlZioJdbcContext[N](naming)
  }
  object MysqlService {
    def apply[N <: NamingStrategy: Tag](naming: N): LayerConstructor[ZLayer[javax.sql.DataSource, Nothing, MysqlService[N]]] =
      new LayerConstructor(ZLayer.fromFunction((ds: javax.sql.DataSource) => new MysqlService[N](naming, ds)))
  }

  case class SqliteService[+N <: NamingStrategy](naming: N, override val ds: DataSource)
    extends Quill[SqliteDialect, N] with SqliteJdbcTypes[N] {
    val dsDelegate = new SqliteZioJdbcContext[N](naming)
  }
  object SqliteService {
    def apply[N <: NamingStrategy: Tag](naming: N): LayerConstructor[ZLayer[javax.sql.DataSource, Nothing, SqliteService[N]]] =
      new LayerConstructor(ZLayer.fromFunction((ds: javax.sql.DataSource) => new SqliteService[N](naming, ds)))
  }

  case class OracleService[+N <: NamingStrategy](naming: N, override val ds: DataSource)
    extends Quill[OracleDialect, N] with OracleJdbcTypes[N] {
    val dsDelegate = new OracleZioJdbcContext[N](naming)
  }
  object OracleService {
    def apply[N <: NamingStrategy: Tag](naming: N): LayerConstructor[ZLayer[javax.sql.DataSource, Nothing, OracleService[N]]] =
      new LayerConstructor(ZLayer.fromFunction((ds: javax.sql.DataSource) => new OracleService[N](naming, ds)))
  }

  object DataSource {
    val live: ZLayer[DataSource, SQLException, Connection] =
      ZLayer.scoped {
        for {
          blockingExecutor <- ZIO.blockingExecutor
          ds <- ZIO.service[DataSource]
          r <- ZioJdbc.scopedBestEffort(ZIO.attempt(ds.getConnection)).refineToOrDie[SQLException].onExecutor(blockingExecutor)
        } yield r
      }

    def fromDataSource(ds: => DataSource): ZLayer[Any, Throwable, DataSource] =
      ZLayer.fromZIO(ZIO.attempt(ds))

    def fromConfig(config: => Config): ZLayer[Any, Throwable, DataSource] =
      fromConfigClosable(config)

    def fromPrefix(prefix: String): ZLayer[Any, Throwable, DataSource] =
      fromPrefixClosable(prefix)

    def fromJdbcConfig(jdbcContextConfig: => JdbcContextConfig): ZLayer[Any, Throwable, DataSource] =
      fromJdbcConfigClosable(jdbcContextConfig)

    def fromConfigClosable(config: => Config): ZLayer[Any, Throwable, DataSource with Closeable] =
      fromJdbcConfigClosable(JdbcContextConfig(config))

    def fromPrefixClosable(prefix: String): ZLayer[Any, Throwable, DataSource with Closeable] =
      fromJdbcConfigClosable(JdbcContextConfig(LoadConfig(prefix)))

    def fromJdbcConfigClosable(jdbcContextConfig: => JdbcContextConfig): ZLayer[Any, Throwable, DataSource with Closeable] =
      ZLayer.scoped {
        for {
          conf <- ZIO.attempt(jdbcContextConfig)
          ds <- scopedBestEffort(ZIO.attempt(conf.dataSource))
        } yield ds
      }
  }

  private[getquill] class LayerConstructor[Layer](val live: Layer)
}

trait Quill[+Dialect <: SqlIdiom, +Naming <: NamingStrategy] extends QuillBaseContext[Dialect, Naming]
