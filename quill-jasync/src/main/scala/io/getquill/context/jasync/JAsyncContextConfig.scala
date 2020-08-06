package io.getquill.context.jasync

import java.nio.charset.Charset
import java.lang.{ Long => JavaLong }

import scala.jdk.CollectionConverters._
import scala.util.Try

import com.github.jasync.sql.db.ConcreteConnection
import com.github.jasync.sql.db.{ ConnectionPoolConfiguration, ConnectionPoolConfigurationBuilder }
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.pool.ObjectFactory
import com.github.jasync.sql.db.util.AbstractURIParser
import com.typesafe.config.Config

abstract class JAsyncContextConfig[C <: ConcreteConnection](
  config:            Config,
  connectionFactory: Configuration => ObjectFactory[C],
  uriParser:         AbstractURIParser
) {

  private def getValue[T](path: String, getter: String => T) = Try(getter(path))
  private def getString(path: String) = getValue(path, config.getString).toOption
  private def getInt(path: String) = getValue(path, config.getInt).toOption
  private def getLong(path: String) = getValue(path, config.getLong).toOption

  private lazy val urlConfiguration: Configuration = getValue("url", config.getString)
    .map(uriParser.parseOrDie(_, uriParser.getDEFAULT.getCharset))
    .getOrElse(uriParser.getDEFAULT)

  private lazy val default = new ConnectionPoolConfigurationBuilder().build()

  lazy val connectionPoolConfiguration = new ConnectionPoolConfiguration(
    getString("host").getOrElse(urlConfiguration.getHost),
    getInt("port").getOrElse(urlConfiguration.getPort),
    getString("database").orElse(Option(urlConfiguration.getDatabase)).orNull,
    getString("username").getOrElse(urlConfiguration.getUsername),
    getString("password").orElse(Option(urlConfiguration.getPassword)).orNull,
    getInt("maxActiveConnections").getOrElse(default.getMaxActiveConnections),
    getLong("maxIdleTime").getOrElse(default.getMaxIdleTime),
    getInt("maxPendingQueries").getOrElse(default.getMaxPendingQueries),
    getLong("connectionValidationInterval").getOrElse(default.getConnectionValidationInterval),
    getLong("connectionCreateTimeout").getOrElse(default.getConnectionCreateTimeout),
    getLong("connectionTestTimeout").getOrElse(default.getConnectionTestTimeout),
    getLong("queryTimeout")
      .orElse(Option(urlConfiguration.getQueryTimeout).map(_.toMillis)).map(JavaLong.valueOf).orNull,
    urlConfiguration.getEventLoopGroup,
    urlConfiguration.getExecutionContext,
    default.getCoroutineDispatcher,
    new SSLConfiguration(
      Map(
        "sslmode" -> getString("sslmode"),
        "sslrootcert" -> getString("sslrootcert")
      ).collect {
          case (key, Some(value)) => key -> value
        }.asJava
    ),
    Try(Charset.forName(config.getString("charset"))).getOrElse(urlConfiguration.getCharset),
    getInt("maximumMessageSize").getOrElse(urlConfiguration.getMaximumMessageSize),
    urlConfiguration.getAllocator,
    getString("applicationName").orElse(Option(urlConfiguration.getApplicationName)).orNull,
    urlConfiguration.getInterceptors,
    getLong("maxConnectionTtl").map(JavaLong.valueOf).orElse(Option(default.getMaxConnectionTtl)).orNull
  )

  def pool =
    new ConnectionPool[C](
      connectionFactory(connectionPoolConfiguration.getConnectionConfiguration),
      connectionPoolConfiguration
    )
}
