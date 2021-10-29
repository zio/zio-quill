package io.getquill.postgres

import io.getquill.util.LoadConfig
import io.getquill.{ JdbcContextConfig, Literal, OuterPostgresZioJdbcContext, Spec }
import zio.Has

import java.io.Closeable
import javax.sql.DataSource

class PeopleZioOuterJdbcSpec extends Spec {
  val testContext = new OuterPostgresZioJdbcContext(Literal)
  import testContext._
  case class Person(name: String, age: Int)

  def ds: DataSource with Closeable = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  "test query" in {
    val q = quote {
      query[Person].filter(p => p.name == "Bert")
    }
    val exec = testContext.run(q).provide(Has(ds))
    println(zio.Runtime.default.unsafeRunSync(exec))
  }

  "test translate" in {
    val q = quote {
      query[Person].filter(p => p.name == "Bert")
    }
    val exec = testContext.translate(q).provide(Has(ds))
    println(zio.Runtime.default.unsafeRunSync(exec))
  }
}
