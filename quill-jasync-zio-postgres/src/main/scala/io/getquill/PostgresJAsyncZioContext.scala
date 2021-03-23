package io.getquill

import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.{QueryResult => DBQueryResult}
import com.typesafe.config.Config
import io.getquill.ReturnAction.{ReturnColumns, ReturnNothing, ReturnRecord}
import io.getquill.context.jasync.{ArrayDecoders, ArrayEncoders, JAsyncContext, JAsyncZioContext, UUIDObjectEncoding}
import io.getquill.util.LoadConfig
import io.getquill.util.Messages.fail
import zio.{Has, Task, ZIO, ZLayer, ZManaged}

import scala.jdk.CollectionConverters._

object PostgresJAsyncZioContext {
  import JAsyncZioContext._

  object BuildPool {
    def fromContextConfig(config: =>PostgresJAsyncContextConfig) =
      ZManaged.make(ZIO.effect(config.pool))(pool => ZIO.effect(pool.disconnect().toZio).flatten.orDie)
    def fromConfig(config: Config) =
      fromContextConfig(PostgresJAsyncContextConfig(config))
    def fromPrefix(prefix: String) =
      fromContextConfig(PostgresJAsyncContextConfig(LoadConfig(prefix)))
  }

  object BuildConnection {
    def fromPool = {
      ZLayer.fromManaged {
        for {
          env <- ZManaged.environment[Has[ConnectionPool[PostgreSQLConnection]]]
          pool = env.get[ConnectionPool[PostgreSQLConnection]]
          conn <- ZManaged.make(ZIO.effect(pool.take().toZio).flatten)(conn => pool.giveBack(conn).toZio.orDie)
        } yield conn
      }
    }
  }
}

class PostgresJAsyncZioContext[N <: NamingStrategy](naming: N)
  extends JAsyncZioContext[PostgresDialect, N](PostgresDialect, naming)
  with PostgresJAsyncContextBase[N] {

  override protected def extractActionResult[O](returningAction: ReturnAction, returningExtractor: Extractor[O])(result: DBQueryResult): O =
    result.getRows.asScala
      .headOption
      .map(returningExtractor)
      .getOrElse(fail("This is a bug. Cannot extract returning value."))

  override protected def expandAction(sql: String, returningAction: ReturnAction): String =
    returningAction match {
      // The Postgres dialect will create SQL that has a 'RETURNING' clause so we don't have to add one.
      case ReturnRecord           => s"$sql"
      // The Postgres dialect will not actually use these below variants but in case we decide to plug
      // in some other dialect into this context...
      case ReturnColumns(columns) => s"$sql RETURNING ${columns.mkString(", ")}"
      case ReturnNothing          => s"$sql"
    }

}
