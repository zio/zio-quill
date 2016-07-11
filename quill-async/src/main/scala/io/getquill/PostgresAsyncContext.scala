package io.getquill

import com.github.mauricio.async.db.{ RowData, QueryResult => DBQueryResult }
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.typesafe.config.Config
import io.getquill.context.async.AsyncContext
import io.getquill.util.LoadConfig

class PostgresAsyncContext[N <: NamingStrategy](pool: PartitionedConnectionPool[PostgreSQLConnection])
  extends AsyncContext[PostgresDialect, N, PostgreSQLConnection](pool) {

  def this(config: PostgresAsyncContextConfig) = this(config.pool)
  def this(config: Config) = this(PostgresAsyncContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override protected def extractActionResult[O](generated: Option[String], returningExtractor: RowData => O)(result: DBQueryResult): O = (generated, result) match {
    case (None, r) =>
      r.rowsAffected.asInstanceOf[O]
    case (Some(col), r) =>
      returningExtractor(r.rows.get(0))

  }

  override protected def expandAction(sql: String, generated: Option[String]): String =
    sql + generated.fold("")(id => s" RETURNING $id")
}
