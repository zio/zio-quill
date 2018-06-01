package io.getquill.context.async.mysql

import java.nio.charset.Charset

import com.typesafe.config.ConfigFactory
import io.getquill.{ MysqlAsyncContextConfig, Spec }

import scala.collection.JavaConverters._
import scala.concurrent.duration._

class MysqlAsyncContextConfigSpec extends Spec {

  "extracts valid data from configs" in {
    val c = ConfigFactory.parseMap(Map(
      "url" -> "jdbc:postgresql://github.com:5233/db?user=p",
      "pass" -> "pass",
      "queryTimeout" -> "123 s",
      "host" -> "github.com",
      "port" -> "5233",
      "charset" -> "UTF-8",
      "user" -> "p",
      "password" -> "pass",
      "maximumMessageSize" -> "456",
      "connectTimeout" -> "789 s"
    ).asJava)
    val conf = MysqlAsyncContextConfig(c)

    conf.queryTimeout mustBe Some(123.seconds)
    conf.connectTimeout mustBe Some(789.seconds)
    conf.maximumMessageSize mustBe Some(456)
    conf.charset mustBe Some(Charset.forName("UTF-8"))
    conf.host mustBe Some("github.com")
    conf.port mustBe Some(5233)
    conf.user mustBe Some("p")
    conf.password mustBe Some("pass")
  }

  "parses url and passes valid data to configuration" in {
    val c = ConfigFactory.parseMap(Map(
      "url" -> "jdbc:mysql://host:5233/db?user=p",
      "pass" -> "pass",
      "queryTimeout" -> "123 s",
      "host" -> "github.com",
      "port" -> "5233",
      "charset" -> "UTF-8",
      "password" -> "pass",
      "maximumMessageSize" -> "456",
      "connectTimeout" -> "789 s"
    ).asJava)
    val conf = MysqlAsyncContextConfig(c)

    conf.configuration.queryTimeout mustBe Some(123.seconds)
    conf.configuration.connectTimeout mustBe 789.seconds
    conf.configuration.maximumMessageSize mustBe 456
    conf.configuration.charset mustBe Charset.forName("UTF-8")
    conf.configuration.host mustBe "github.com"
    conf.configuration.port mustBe 5233
    conf.configuration.username mustBe "p"
    conf.configuration.password mustBe Some("pass")
  }

}
