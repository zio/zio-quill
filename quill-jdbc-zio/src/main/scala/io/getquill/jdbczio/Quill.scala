package io.getquill.jdbczio

import com.typesafe.config.Config
import io.getquill._
import io.getquill.context.ZioJdbc
import io.getquill.context.ZioJdbc.scopedBestEffort
import io.getquill.context.jdbc._
import io.getquill.context.json.PostgresJsonExtensions
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.util.LoadConfig
import zio.{Tag, ZIO, ZLayer}

import java.io.Closeable
import java.sql.{Connection, SQLException}
import javax.sql.DataSource

object Quill {
  class Postgres[+N <: NamingStrategy](val naming: N, override val ds: DataSource)
      extends Quill[PostgresDialect, N]
      with PostgresJdbcTypes[PostgresDialect, N]
      with PostgresJsonExtensions {
    val idiom: PostgresDialect = PostgresDialect
    val dsDelegate             = new PostgresZioJdbcContext[N](naming)
  }

  /** Postgres ZIO Context without JDBC Encoders */
  class PostgresLite[+N <: NamingStrategy](val naming: N, override val ds: DataSource)
      extends Quill[PostgresDialect, N]
      with PostgresJdbcTypes[PostgresDialect, N] {
    val idiom: PostgresDialect = PostgresDialect
    val dsDelegate             = new PostgresZioJdbcContext[N](naming)
  }

  object Postgres {
    def apply[N <: NamingStrategy](naming: N, ds: DataSource) = new PostgresLite[N](naming, ds)
    def fromNamingStrategy[N <: NamingStrategy: Tag](naming: N): ZLayer[javax.sql.DataSource, Nothing, Postgres[N]] =
      ZLayer.fromFunction((ds: javax.sql.DataSource) => new Postgres[N](naming, ds))
  }

  class SqlServer[+N <: NamingStrategy](val naming: N, override val ds: DataSource)
      extends Quill[SQLServerDialect, N]
      with SqlServerJdbcTypes[SQLServerDialect, N] {
    val idiom: SQLServerDialect = SQLServerDialect
    val dsDelegate              = new SqlServerZioJdbcContext[N](naming)
  }
  object SqlServer {
    def apply[N <: NamingStrategy](naming: N, ds: DataSource) = new SqlServer[N](naming, ds)
    def fromNamingStrategy[N <: NamingStrategy: Tag](naming: N): ZLayer[javax.sql.DataSource, Nothing, SqlServer[N]] =
      ZLayer.fromFunction((ds: javax.sql.DataSource) => new SqlServer[N](naming, ds))
  }

  class H2[+N <: NamingStrategy](val naming: N, override val ds: DataSource)
      extends Quill[H2Dialect, N]
      with H2JdbcTypes[H2Dialect, N] {
    val idiom: H2Dialect = H2Dialect
    val dsDelegate       = new H2ZioJdbcContext[N](naming)
  }
  object H2 {
    def apply[N <: NamingStrategy](naming: N, ds: DataSource) = new H2[N](naming, ds)
    def fromNamingStrategy[N <: NamingStrategy: Tag](naming: N): ZLayer[javax.sql.DataSource, Nothing, H2[N]] =
      ZLayer.fromFunction((ds: javax.sql.DataSource) => new H2[N](naming, ds))
  }

  class Mysql[+N <: NamingStrategy](val naming: N, override val ds: DataSource)
      extends Quill[MySQLDialect, N]
      with MysqlJdbcTypes[MySQLDialect, N] {
    val idiom: MySQLDialect = MySQLDialect
    val dsDelegate          = new MysqlZioJdbcContext[N](naming)
  }
  object Mysql {
    def apply[N <: NamingStrategy](naming: N, ds: DataSource) = new Mysql[N](naming, ds)
    def fromNamingStrategy[N <: NamingStrategy: Tag](naming: N): ZLayer[javax.sql.DataSource, Nothing, Mysql[N]] =
      ZLayer.fromFunction((ds: javax.sql.DataSource) => new Mysql[N](naming, ds))
  }

  class Sqlite[+N <: NamingStrategy](val naming: N, override val ds: DataSource)
      extends Quill[SqliteDialect, N]
      with SqliteJdbcTypes[SqliteDialect, N] {
    val idiom: SqliteDialect = SqliteDialect
    val dsDelegate           = new SqliteZioJdbcContext[N](naming)
  }
  object Sqlite {
    def apply[N <: NamingStrategy](naming: N, ds: DataSource) = new Sqlite[N](naming, ds)
    def fromNamingStrategy[N <: NamingStrategy: Tag](naming: N): ZLayer[javax.sql.DataSource, Nothing, Sqlite[N]] =
      ZLayer.fromFunction((ds: javax.sql.DataSource) => new Sqlite[N](naming, ds))
  }

  class Oracle[+N <: NamingStrategy](val naming: N, override val ds: DataSource)
      extends Quill[OracleDialect, N]
      with OracleJdbcTypes[OracleDialect, N] {
    val idiom: OracleDialect = OracleDialect
    val dsDelegate           = new OracleZioJdbcContext[N](naming)
  }
  object Oracle {
    def apply[N <: NamingStrategy](naming: N, ds: DataSource) = new Oracle[N](naming, ds)
    def fromNamingStrategy[N <: NamingStrategy: Tag](naming: N): ZLayer[javax.sql.DataSource, Nothing, Oracle[N]] =
      ZLayer.fromFunction((ds: javax.sql.DataSource) => new Oracle[N](naming, ds))
  }

  object Connection {
    val acquire: ZLayer[DataSource, SQLException, Connection] =
      ZLayer.scoped {
        for {
          ds <- ZIO.service[DataSource]
          c <- ZIO.blocking {
                 ZioJdbc
                   .scopedBestEffort(ZIO.attempt(ds.getConnection))
                   .refineToOrDie[SQLException]
               }
        } yield c
      }
  }

  object DataSource {
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

    def fromJdbcConfigClosable(
      jdbcContextConfig: => JdbcContextConfig
    ): ZLayer[Any, Throwable, DataSource with Closeable] =
      ZLayer.scoped {
        for {
          conf <- ZIO.attempt(jdbcContextConfig)
          ds   <- scopedBestEffort(ZIO.attempt(conf.dataSource))
        } yield ds
      }
  }
}

trait Quill[+Dialect <: SqlIdiom, +Naming <: NamingStrategy] extends QuillBaseContext[Dialect, Naming]
