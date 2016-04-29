package io.getquill.sources.async

import com.github.mauricio.async.db.Connection
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.SqlIdiom

class TransactionalAsyncSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](config: AsyncSourceConfig[D, N, C], conn: Connection)
  extends AsyncSource[D, N, C](config) {

  def connection: Connection = conn
}