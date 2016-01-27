package io.getquill.sources.async

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
      val default = PoolConfiguration.Default
      val maxObjects = Try(config.getInt("poolMaxObjects")).getOrElse(default.maxObjects)
      val maxIdle = Try(config.getLong("poolMaxIdle")).getOrElse(default.maxIdle)
      val maxQueueSize = Try(config.getInt("poolMaxQueueSize")).getOrElse(default.maxQueueSize)
      val validationInterval = Try(config.getLong("poolValidationInterval")).getOrElse(default.validationInterval)
      PoolConfiguration(
        maxObjects = maxObjects,
        maxIdle = maxIdle,
        maxQueueSize = maxQueueSize,
        validationInterval = validationInterval)
    }

    val numberOfPartitions = Try(config.getInt("poolNumberOfPartitions")).getOrElse(4)

    new PartitionedConnectionPool[C](
      connectionFactory(configuration),
      poolConfiguration,
      numberOfPartitions)
  }
}
