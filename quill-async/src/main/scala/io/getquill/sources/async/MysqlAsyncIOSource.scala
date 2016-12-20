package io.getquill.sources.async

import com.github.mauricio.async.db.mysql.MySQLQueryResult
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult, Connection }

import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.SqlIdiom

class MysqlAsyncIOSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](pool: AsyncPool) extends AsyncIOSource[D, N, C](pool) {

  override protected def extractActionResult(generated: Option[String])(result: DBQueryResult): Long = {
    (generated, result) match {
      case (None, r)                      => r.rowsAffected
      case (Some(_), r: MySQLQueryResult) => r.lastInsertId
      case _                              => throw new IllegalStateException("This is a bug. Cannot extract generated value.")
    }
  }
}
