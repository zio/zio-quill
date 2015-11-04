package io.getquill.source.async

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.pool.ObjectFactory
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.github.mauricio.async.db.pool.PoolConfiguration
import com.typesafe.config.Config
import scala.util.Try

object Pool {

  def apply[C <: Connection](config: Config, connectionFactory: Configuration => ObjectFactory[C]) = {

    val configuration =
      new Configuration(
        username = config.getString("user"),
        password = Try(config.getString("password")).toOption,
        database = Try(config.getString("database")).toOption,
        port = config.getInt("port"),
        host = config.getString("host"))

    val poolConfiguration = {
      var poolConfig = PoolConfiguration.Default
      Try(config.getInt("poolMaxQueueSize")).map { value =>
        poolConfig = poolConfig.copy(maxQueueSize = value)
      }
      Try(config.getInt("poolMaxObjects")).map { value =>
        poolConfig = poolConfig.copy(maxObjects = value)
      }
      Try(config.getLong("poolMaxIdle")).map { value =>
        poolConfig = poolConfig.copy(maxIdle = value)
      }
      poolConfig
    }

    val numberOfPartitions = Try(config.getInt("poolNumberOfPartitions")).getOrElse(4)

    new PartitionedConnectionPool[C](
      connectionFactory(configuration),
      poolConfiguration,
      numberOfPartitions)
  }
}
