package io.getquill

import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.mysql.{ MySQLQueryResult, MySQLConnection }
import com.github.mauricio.async.db.mysql.pool.MySQLConnectionFactory
import io.getquill.naming.NamingStrategy
import io.getquill.sources.SourceConfig
import io.getquill.sources.async.{ PoolAsyncSource, AsyncSourceConfig }
import io.getquill.sources.sql.idiom.MySQLDialect

class MysqlAsyncSourceConfig[N <: NamingStrategy](name: String)
  extends AsyncSourceConfig[MySQLDialect, N, MySQLConnection](name, new MySQLConnectionFactory(_))
  with SourceConfig[PoolAsyncSource[MySQLDialect, N, MySQLConnection]] {

  def extractActionResult(generated: Option[String])(result: DBQueryResult): Long = {
    (generated, result) match {
      case (None, r)                      => r.rowsAffected
      case (Some(_), r: MySQLQueryResult) => r.lastInsertId
      case _                              => throw new IllegalStateException("This is a bug. Cannot extract generated value.")
    }
  }
}