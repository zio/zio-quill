package io.getquill

import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
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

  override protected def extractActionResult(generated: Option[String])(result: DBQueryResult): Long = (generated, result) match {
    case (None, r) =>
      r.rowsAffected
    case (Some(col), r) =>
      r.rows.get(0)(col) match {
        case l: Long => l
        case i: Int  => i.toLong
        case other =>
          throw new IllegalArgumentException(s"Type ${other.getClass.toString} of column $col is not supported for returning values")
      }
  }

  override protected def expandAction(sql: String, generated: Option[String]): String =
    sql + generated.fold("")(id => s" RETURNING $id")
}
