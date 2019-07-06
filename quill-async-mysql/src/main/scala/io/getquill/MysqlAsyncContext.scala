package io.getquill

import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.MySQLQueryResult
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.typesafe.config.Config
import io.getquill.context.async.{ AsyncContext, UUIDStringEncoding }
import io.getquill.util.LoadConfig
import io.getquill.util.Messages.fail
import com.github.mauricio.async.db.general.ArrayRowData

class MysqlAsyncContext[N <: NamingStrategy](naming: N, pool: PartitionedConnectionPool[MySQLConnection])
  extends AsyncContext[MySQLDialect, N, MySQLConnection](MySQLDialect, naming, pool) with UUIDStringEncoding {

  def this(naming: N, config: MysqlAsyncContextConfig) = this(naming, config.pool)
  def this(naming: N, config: Config) = this(naming, MysqlAsyncContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  override protected def extractActionResult[O](returningAction: ReturnAction, returningExtractor: Extractor[O])(result: DBQueryResult): O = {
    result match {
      case r: MySQLQueryResult =>
        returningExtractor(new ArrayRowData(0, Map.empty, Array(r.lastInsertId)))
      case _ =>
        fail("This is a bug. Cannot extract returning value.")
    }
  }
}
