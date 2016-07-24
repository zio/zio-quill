package io.getquill

import com.github.mauricio.async.db.{ RowData, QueryResult => DBQueryResult }
import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.MySQLQueryResult
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.typesafe.config.Config
import io.getquill.context.async.AsyncContext
import io.getquill.util.LoadConfig

class MysqlAsyncContext[N <: NamingStrategy](pool: PartitionedConnectionPool[MySQLConnection])
  extends AsyncContext[MySQLDialect, N, MySQLConnection](pool) {

  def this(config: MysqlAsyncContextConfig) = this(config.pool)
  def this(config: Config) = this(MysqlAsyncContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override protected def extractActionResult[O](generated: Option[String], returningExtractor: RowData => O)(result: DBQueryResult): O = {
    (generated, result) match {
      case (None, r)                      => r.rowsAffected.asInstanceOf[O]
      case (Some(_), r: MySQLQueryResult) => r.lastInsertId.asInstanceOf[O]
      case _                              => throw new IllegalStateException("This is a bug. Cannot extract returning value.")
    }
  }

}
