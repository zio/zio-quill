package io.getquill.sources.async

import com.github.mauricio.async.db.Connection
import io.getquill.naming.NamingStrategy
import io.getquill.sources.sql.idiom.SqlIdiom

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class PoolAsyncSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](config: AsyncSourceConfig[D, N, C])
  extends AsyncSource[D, N, C](config) {

  private val pool = config.pool

  def connection: Connection = config.pool

  override def close = {
    Await.result(pool.close, Duration.Inf)
    ()
  }
}
