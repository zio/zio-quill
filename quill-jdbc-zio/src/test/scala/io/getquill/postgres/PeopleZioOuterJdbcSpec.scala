package io.getquill.postgres

import io.getquill.util.LoadConfig
import io.getquill.{ JdbcContextConfig, Literal, PostgresZioJdbcContext, Spec }
import zio.{ Unsafe, ZEnvironment, Runtime }

class PeopleZioOuterJdbcSpec extends Spec {
  val testContext = new PostgresZioJdbcContext(Literal)
  import testContext._
  case class Person(name: String, age: Int)

  def ds = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  "test query" in {
    val q = quote {
      query[Person].filter(p => p.name == "Bert")
    }
    val exec = testContext.run(q).provideEnvironment(ZEnvironment(ds))
    println(Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(exec).getOrThrow()
    })
  }

  "test translate" in {
    val q = quote {
      query[Person].filter(p => p.name == "Bert")
    }
    val a = testContext.translate(q)
    val exec = a.provideEnvironment(ZEnvironment(ds))
    println(Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(exec).getOrThrow()
    })
  }
}
