package io.getquill

import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.typesafe.config.Config
import io.getquill.ReturnAction.{ ReturnColumns, ReturnNothing, ReturnRecord }
import io.getquill.context.async.{ ArrayDecoders, ArrayEncoders, AsyncContext, UUIDObjectEncoding }
import io.getquill.util.LoadConfig
import io.getquill.util.Messages.fail

class PostgresAsyncContext[N <: NamingStrategy](naming: N, pool: PartitionedConnectionPool[PostgreSQLConnection])
  extends AsyncContext[PostgresDialect, N, PostgreSQLConnection](PostgresDialect, naming, pool)
  with ArrayEncoders
  with ArrayDecoders
  with UUIDObjectEncoding {

  def this(naming: N, config: PostgresAsyncContextConfig) = this(naming, config.pool)
  def this(naming: N, config: Config) = this(naming, PostgresAsyncContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  override protected def extractActionResult[O](returningAction: ReturnAction, returningExtractor: Extractor[O])(result: DBQueryResult): O = {
    result.rows match {
      case Some(r) if r.nonEmpty =>
        returningExtractor(r.head)
      case _ =>
        fail("This is a bug. Cannot extract returning value.")
    }
  }

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
