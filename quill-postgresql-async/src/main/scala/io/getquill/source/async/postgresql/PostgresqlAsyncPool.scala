package io.getquill.source.async.postgresql

import com.typesafe.config.Config
import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.postgresql.pool.PostgreSQLConnectionFactory
import com.github.mauricio.async.db.pool.PoolConfiguration
import com.github.mauricio.async.db.pool.PartitionedConnectionPool

object PostgresqlAsyncPool {

  def apply(config: Config) = {

    val configuration =
      new Configuration(
        username = config.getString("user"),
        password = Option(config.getString("password")),
        database = Option(config.getString("database")),
        host = config.getString("host"))

    val poolConfiguration = {
      var poolConfig = PoolConfiguration.Default
      Option(config.getInt("poolMaxQueueSize")).map { value =>
        poolConfig = poolConfig.copy(maxQueueSize = value)
      }
      Option(config.getInt("poolMaxObjects")).map { value =>
        poolConfig = poolConfig.copy(maxObjects = value)
      }
      Option(config.getLong("poolMaxIdle")).map { value =>
        poolConfig = poolConfig.copy(maxIdle = value)
      }
      poolConfig
    }

    val numberOfPartitions = Option(config.getInt("poolNumberOfPartitions")).getOrElse(4)

    new PartitionedConnectionPool(
      new PostgreSQLConnectionFactory(configuration),
      poolConfiguration,
      numberOfPartitions)
  }
}
