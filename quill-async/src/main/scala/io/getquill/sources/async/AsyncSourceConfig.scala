package io.getquill.sources.async

import io.getquill.sources.SourceConfig
import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.pool.ObjectFactory
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.github.mauricio.async.db.pool.PoolConfiguration
import com.typesafe.config.Config
import scala.util.Try
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.naming.NamingStrategy

abstract class AsyncSourceConfig[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](
  val name: String,
  connectionFactory: Configuration => ObjectFactory[C])
    extends SourceConfig[AsyncSource[D, N, C]] {

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
      host = host)

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
      validationInterval = poolValidationInterval)

  def numberOfPartitions = Try(config.getInt("poolNumberOfPartitions")).getOrElse(4)

  def pool =
    new PartitionedConnectionPool[C](
      connectionFactory(configuration),
      poolConfiguration,
      numberOfPartitions)
}
