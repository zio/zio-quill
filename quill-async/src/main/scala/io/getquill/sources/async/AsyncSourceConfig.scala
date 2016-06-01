package io.getquill.sources.async

import com.github.mauricio.async.db.pool.PoolConfiguration

import scala.util.Try
import com.typesafe.config.Config
import com.github.mauricio.async.db.Configuration

case class AsyncSourceConfig(config: Config) {

  def user = config.getString("user")
  def password = Try(config.getString("password")).toOption
  def database = Try(config.getString("database")).toOption
  def port = config.getInt("port")
  def host = config.getString("host")

  def configuration =
    new Configuration(
      username = user,
      password = password,
      database = database,
      port = port,
      host = host
    )

  private val defaultPoolConfig = PoolConfiguration.Default

  def poolMaxObjects = Try(config.getInt("poolMaxObjects")).getOrElse(defaultPoolConfig.maxObjects)
  def poolMaxIdle = Try(config.getLong("poolMaxIdle")).getOrElse(defaultPoolConfig.maxIdle)
  def poolMaxQueueSize = Try(config.getInt("poolMaxQueueSize")).getOrElse(defaultPoolConfig.maxQueueSize)
  def poolValidationInterval = Try(config.getLong("poolValidationInterval")).getOrElse(defaultPoolConfig.validationInterval)

  def poolConfiguration =
    PoolConfiguration(
      maxObjects = poolMaxObjects,
      maxIdle = poolMaxIdle,
      maxQueueSize = poolMaxQueueSize,
      validationInterval = poolValidationInterval
    )

  def numberOfPartitions = Try(config.getInt("poolNumberOfPartitions")).getOrElse(4)
}
