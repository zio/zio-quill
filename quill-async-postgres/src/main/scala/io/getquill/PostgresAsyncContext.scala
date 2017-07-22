package io.getquill

import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.typesafe.config.Config
import io.getquill.context.async.{ ArrayDecoders, ArrayEncoders, AsyncContext, UUIDObjectEncoding }
import io.getquill.util.LoadConfig
import io.getquill.util.Messages.fail

@deprecated("The postgresql-async driver is not maintained anymore. Consider migrating to the NDBC module.", "11/2018")
class PostgresAsyncContext[N <: NamingStrategy](naming: N, pool: PartitionedConnectionPool[PostgreSQLConnection])
  extends AsyncContext(PostgresDialect, naming, pool)
  with ArrayEncoders
  with ArrayDecoders
  with UUIDObjectEncoding {

  def this(naming: N, config: PostgresAsyncContextConfig) = this(naming, config.pool)
  def this(naming: N, config: Config) = this(naming, PostgresAsyncContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  override protected def extractActionResult[O](returningColumn: String, returningExtractor: Extractor[O])(result: DBQueryResult): O = {
    result.rows match {
      case Some(r) if r.nonEmpty =>
        returningExtractor(r.head)
      case _ =>
        fail("This is a bug. Cannot extract returning value.")
    }
  }

  override protected def expandAction(sql: String, returningColumn: String): String =
    s"$sql RETURNING $returningColumn"
}
