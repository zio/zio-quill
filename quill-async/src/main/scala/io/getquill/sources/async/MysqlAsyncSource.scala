package io.getquill.sources.async

import com.github.mauricio.async.db.mysql.MySQLQueryResult
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult, Connection }

import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.SqlIdiom

class MysqlAsyncSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](config: AsyncSourceConfig[D, N, C]) extends AsyncSource[D, N, C](config) {

  override protected def extractActionResult(generated: Option[String])(result: DBQueryResult): Long = {
    (generated, result) match {
      case (None, r)                      => r.rowsAffected
      case (Some(_), r: MySQLQueryResult) => r.lastInsertId
      case _                              => throw new IllegalStateException("This is a bug. Cannot extract generated value.")
    }
  }
}
