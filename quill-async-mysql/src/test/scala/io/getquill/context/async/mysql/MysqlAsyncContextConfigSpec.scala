package io.getquill.context.async.mysql

import java.nio.charset.Charset

import com.typesafe.config.ConfigFactory
import io.getquill.{ MysqlAsyncContextConfig, Spec }

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

class MysqlAsyncContextConfigSpec extends Spec {
  class AsyncContextConfigSpec extends Spec {
    "parses url" in {
      val c = ConfigFactory.parseMap(Map(
        "url" -> "jdbc:postgresql://host:5233/db?user=p",
        "pass" -> "pass",
        "queryTimeout" -> "123",
        "charset" -> "UTF-8",
        "maximumMessageSize" -> "123",
        "connectTimeout" -> "123"
      ).asJava)
      val conf = MysqlAsyncContextConfig(c)

      conf.queryTimeout mustBe Some(Duration("123"))
      conf.connectTimeout mustBe Some(Duration("123"))
      conf.maximumMessageSize mustBe Some(123)
      conf.charset mustBe Some(Charset.forName("UTF-8"))
      conf.host mustBe Some("123")
      conf.user mustBe Some("p")
      conf.password mustBe Some("pass")
    }
  }

}
