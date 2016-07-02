package io.getquill.sources.async

import com.github.mauricio.async.db.{ QueryResult => DBQueryResult, Connection }
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.SqlIdiom

class PostgresAsyncSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](config: AsyncSourceConfig[D, N, C]) extends AsyncSource[D, N, C](config) {

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
