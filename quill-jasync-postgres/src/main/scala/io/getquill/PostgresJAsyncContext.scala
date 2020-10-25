package io.getquill

import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.{ QueryResult => DBQueryResult }
import com.typesafe.config.Config
import io.getquill.ReturnAction.{ ReturnColumns, ReturnNothing, ReturnRecord }
import io.getquill.context.jasync.{ ArrayDecoders, ArrayEncoders, JAsyncContext, UUIDObjectEncoding }
import io.getquill.util.LoadConfig
import io.getquill.util.Messages.fail
import scala.jdk.CollectionConverters._

class PostgresJAsyncContext[N <: NamingStrategy](naming: N, pool: ConnectionPool[PostgreSQLConnection])
  extends JAsyncContext[PostgresDialect, N, PostgreSQLConnection](PostgresDialect, naming, pool)
  with ArrayEncoders
  with ArrayDecoders
  with UUIDObjectEncoding {

  def this(naming: N, config: PostgresJAsyncContextConfig) = this(naming, config.pool)
  def this(naming: N, config: Config) = this(naming, PostgresJAsyncContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

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
