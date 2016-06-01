package io.getquill.sources.async

import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.mysql.MySQLConnection
import com.github.mauricio.async.db.mysql.MySQLQueryResult
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import com.typesafe.config.Config

import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.MySQLDialect
import io.getquill.util.LoadConfig

class MysqlAsyncSource[N <: NamingStrategy](config: AsyncSourceConfig)
  extends PoolAsyncSource[MySQLDialect, N, MySQLConnection](config, new MySQLConnectionFactory(_)) {

  def this(config: Config) = this(AsyncSourceConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

  def extractActionResult(generated: Option[String])(result: DBQueryResult): Long = {
    (generated, result) match {
      case (None, r)                      => r.rowsAffected
      case (Some(_), r: MySQLQueryResult) => r.lastInsertId
      case _                              => throw new IllegalStateException("This is a bug. Cannot extract generated value.")
    }
  }
}
