package io.getquill

import com.github.mauricio.async.db.{ RowData, QueryResult => DBQueryResult }
import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.MySQLQueryResult
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.typesafe.config.Config
import io.getquill.context.async.AsyncContext
import io.getquill.util.LoadConfig
import com.github.mauricio.async.db.general.ArrayRowData

class MysqlAsyncContext[N <: NamingStrategy](pool: PartitionedConnectionPool[MySQLConnection])
  extends AsyncContext[MySQLDialect, N, MySQLConnection](pool) {

  def this(config: MysqlAsyncContextConfig) = this(config.pool)
  def this(config: Config) = this(MysqlAsyncContextConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  override protected def extractActionResult[O](returningColumn: String, returningExtractor: RowData => O)(result: DBQueryResult): O = {
    result match {
      case r: MySQLQueryResult =>
        returningExtractor(new ArrayRowData(0, Map.empty, Array(r.lastInsertId)))
      case _ =>
        throw new IllegalStateException("This is a bug. Cannot extract returning value.")
    }
  }
}
