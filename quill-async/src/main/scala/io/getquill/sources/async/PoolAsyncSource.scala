package io.getquill.sources.async

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.pool.ObjectFactory
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.naming.NamingStrategy

abstract class PoolAsyncSource[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](config: AsyncSourceConfig, connectionFactory: Configuration => ObjectFactory[C])
  extends AsyncSource[D, N, C] {

  override protected val connection =
    new PartitionedConnectionPool[C](
      connectionFactory(config.configuration),
      config.poolConfiguration,
      config.numberOfPartitions
    )

  override def close = {
    Await.result(connection.close, Duration.Inf)
    ()
  }

}

