package io.getquill.context.jasync.mysql

import java.nio.charset.Charset

import com.typesafe.config.ConfigFactory
import io.getquill.{ MysqlJAsyncContextConfig, Spec }

import scala.jdk.CollectionConverters._

class MysqlJAsyncContextConfigSpec extends Spec {

  "extracts valid data from configs" in {
    val c = ConfigFactory.parseMap(Map(
      "url" -> "jdbc:postgresql://github.com:5233/db?user=p",
      "pass" -> "pass",
      "queryTimeout" -> "123",
      "host" -> "github.com",
      "port" -> "5233",
      "charset" -> "UTF-8",
      "username" -> "p",
      "password" -> "pass",
      "maximumMessageSize" -> "456",
      "connectionTestTimeout" -> "789"
    ).asJava)
    val conf = MysqlJAsyncContextConfig(c).connectionPoolConfiguration

    conf.getQueryTimeout mustBe 123L
    conf.getConnectionTestTimeout mustBe 789L
    conf.getMaximumMessageSize mustBe 456
    conf.getCharset mustBe Charset.forName("UTF-8")
    conf.getHost mustBe "github.com"
    conf.getPort mustBe 5233
    conf.getUsername mustBe "p"
    conf.getPassword mustBe "pass"
  }

  "parses url and passes valid data to configuration" in {
    val c = ConfigFactory.parseMap(Map(
      "url" -> "jdbc:mysql://host:5233/db?user=p",
      "pass" -> "pass",
      "queryTimeout" -> "123",
      "host" -> "github.com",
      "port" -> "5233",
      "charset" -> "UTF-8",
      "password" -> "pass",
      "maximumMessageSize" -> "456",
      "connectionTestTimeout" -> "789"
    ).asJava)
    val conf = MysqlJAsyncContextConfig(c).connectionPoolConfiguration

    conf.getQueryTimeout mustBe 123L
    conf.getConnectionTestTimeout mustBe 789L
    conf.getMaximumMessageSize mustBe 456
    conf.getCharset mustBe Charset.forName("UTF-8")
    conf.getHost mustBe "github.com"
    conf.getPort mustBe 5233
    conf.getUsername mustBe "p"
    conf.getPassword mustBe "pass"
  }

}
