package io.getquill.sources.async

import com.github.mauricio.async.db.{ Configuration, Connection, QueryResult }
import com.github.mauricio.async.db.pool.{ ObjectFactory, PartitionedConnectionPool, PoolConfiguration }
import io.getquill.naming.NamingStrategy
import io.getquill.sources.SourceConfig
import io.getquill.sources.sql.idiom.SqlIdiom

import scala.util.Try

abstract class AsyncSourceConfig[D <: SqlIdiom, N <: NamingStrategy, C <: Connection](
  val name:          String,
  connectionFactory: Configuration => ObjectFactory[C]
) {

  this: SourceConfig[_] =>

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

  def extractActionResult(generated: Option[String])(result: QueryResult): Long

  def expandAction(sql: String, generated: Option[String]) = sql

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

  def pool =
    new PartitionedConnectionPool[C](
      connectionFactory(configuration),
      poolConfiguration,
      numberOfPartitions
    )
}
