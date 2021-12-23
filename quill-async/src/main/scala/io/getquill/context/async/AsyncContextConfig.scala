package io.getquill.context.async

import java.nio.charset.Charset

import com.github.mauricio.async.db.Configuration
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.SSLConfiguration
import com.github.mauricio.async.db.pool.ObjectFactory
import com.github.mauricio.async.db.pool.PartitionedConnectionPool
import com.github.mauricio.async.db.pool.PoolConfiguration
import com.github.mauricio.async.db.util.AbstractURIParser
import com.typesafe.config.Config

import scala.concurrent.duration.Duration
import scala.util.Try

abstract class AsyncContextConfig[C <: Connection](
  config:            Config,
  connectionFactory: Configuration => ObjectFactory[C],
  uriParser:         AbstractURIParser
) {
  def url: Option[String] = Try(config.getString("url")).toOption
  def user: Option[String] = Try(config.getString("user")).toOption
  def password: Option[String] = Try(config.getString("password")).toOption
  def database: Option[String] = Try(config.getString("database")).toOption
  def port: Option[Int] = Try(config.getInt("port")).toOption
  def host: Option[String] = Try(config.getString("host")).toOption
  def sslProps: Map[String,String] = Map(
    "sslmode" -> Try(config.getString("sslmode")).toOption,
    "sslrootcert" -> Try(config.getString("sslrootcert")).toOption
  ).collect { case (key, Some(value)) => key -> value }
  def charset: Option[Charset] = Try(Charset.forName(config.getString("charset"))).toOption
  def maximumMessageSize: Option[Int] = Try(config.getInt("maximumMessageSize")).toOption
  def connectTimeout: Option[Duration] = Try(Duration(config.getString("connectTimeout"))).toOption
  def testTimeout: Option[Duration] = Try(Duration(config.getString("testTimeout"))).toOption
  def queryTimeout: Option[Duration] = Try(Duration(config.getString("queryTimeout"))).toOption

  def configuration: Configuration = {
    var c =
      url match {
        case Some(url) => uriParser.parseOrDie(url)
        case _         => uriParser.DEFAULT
      }
    user.foreach(p => c = c.copy(username = p))
    if (password.nonEmpty) {
      c = c.copy(password = password)
    }
    if (database.nonEmpty) {
      c = c.copy(database = database)
    }
    port.foreach(p => c = c.copy(port = p))
    host.foreach(p => c = c.copy(host = p))
    c = c.copy(ssl = SSLConfiguration(sslProps))
    charset.foreach(p => c = c.copy(charset = p))
    maximumMessageSize.foreach(p => c = c.copy(maximumMessageSize = p))
    connectTimeout.foreach(p => c = c.copy(connectTimeout = p))
    testTimeout.foreach(p => c = c.copy(testTimeout = p))
    c = c.copy(queryTimeout = queryTimeout)
    c
  }

  private val defaultPoolConfig = PoolConfiguration.Default

  def poolMaxObjects: Int = Try(config.getInt("poolMaxObjects")).getOrElse(defaultPoolConfig.maxObjects)
  def poolMaxIdle: Long = Try(config.getLong("poolMaxIdle")).getOrElse(defaultPoolConfig.maxIdle)
  def poolMaxQueueSize: Int = Try(config.getInt("poolMaxQueueSize")).getOrElse(defaultPoolConfig.maxQueueSize)
  def poolValidationInterval: Long = Try(config.getLong("poolValidationInterval")).getOrElse(defaultPoolConfig.validationInterval)

  def poolConfiguration: PoolConfiguration =
    PoolConfiguration(
      maxObjects = poolMaxObjects,
      maxIdle = poolMaxIdle,
      maxQueueSize = poolMaxQueueSize,
      validationInterval = poolValidationInterval
    )

  def numberOfPartitions: Int = Try(config.getInt("poolNumberOfPartitions")).getOrElse(4)

  def pool =
    new PartitionedConnectionPool[C](
      connectionFactory(configuration),
      poolConfiguration,
      numberOfPartitions
    )
}
