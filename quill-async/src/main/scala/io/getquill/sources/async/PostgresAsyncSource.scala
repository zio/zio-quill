package io.getquill.sources.async

import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.PostgresDialect
import com.github.mauricio.async.db.{ QueryResult => DBQueryResult }
import com.typesafe.config.Config
import io.getquill.util.LoadConfig

class PostgresAsyncSource[N <: NamingStrategy](config: AsyncSourceConfig)
  extends PoolAsyncSource[PostgresDialect, N, PostgreSQLConnection](config, new PostgreSQLConnectionFactory(_)) {

  def this(config: Config) = this(AsyncSourceConfig(config))
  def this(configPrefix: String) = this(LoadConfig(configPrefix))

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
