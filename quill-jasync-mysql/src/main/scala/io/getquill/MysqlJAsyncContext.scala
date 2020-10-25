package io.getquill

import java.lang.{ Long => JavaLong }

import com.github.jasync.sql.db.{ QueryResult => DBQueryResult }
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.mysql.MySQLQueryResult
import com.github.jasync.sql.db.pool.ConnectionPool
import com.typesafe.config.Config
import io.getquill.context.jasync.{ JAsyncContext, UUIDStringEncoding }
import io.getquill.util.LoadConfig
import io.getquill.util.Messages.fail
import com.github.jasync.sql.db.general.ArrayRowData
import scala.jdk.CollectionConverters._

class MysqlJAsyncContext[N <: NamingStrategy](naming: N, pool: ConnectionPool[MySQLConnection])
  extends JAsyncContext[MySQLDialect, N, MySQLConnection](MySQLDialect, naming, pool) with UUIDStringEncoding {

  def this(naming: N, config: MysqlJAsyncContextConfig) = this(naming, config.pool)
  def this(naming: N, config: Config) = this(naming, MysqlJAsyncContextConfig(config))
  def this(naming: N, configPrefix: String) = this(naming, LoadConfig(configPrefix))

  override protected def extractActionResult[O](returningAction: ReturnAction, returningExtractor: Extractor[O])(result: DBQueryResult): O = {
    result match {
      case r: MySQLQueryResult =>
        returningExtractor(new ArrayRowData(0, Map.empty[String, Integer].asJava, Array(JavaLong.valueOf(r.getLastInsertId))))
      case _ =>
        fail("This is a bug. Cannot extract returning value.")
    }
  }
}
