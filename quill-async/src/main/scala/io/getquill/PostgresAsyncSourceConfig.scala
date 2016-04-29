package io.getquill

import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import io.getquill.naming.NamingStrategy
import io.getquill.sources.SourceConfig
import io.getquill.sources.async.{ AsyncSourceConfig, PoolAsyncSource }
import io.getquill.sources.sql.idiom.PostgresDialect

class PostgresAsyncSourceConfig[N <: NamingStrategy](name: String)
  extends AsyncSourceConfig[PostgresDialect, N, PostgreSQLConnection](name, new PostgreSQLConnectionFactory(_))
  with SourceConfig[PoolAsyncSource[PostgresDialect, N, PostgreSQLConnection]] {

  def extractActionResult(generated: Option[String])(result: DBQueryResult): Long = (generated, result) match {
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

  override def expandAction(sql: String, generated: Option[String]): String =
    sql + generated.fold("")(id => s" RETURNING $id")
}
