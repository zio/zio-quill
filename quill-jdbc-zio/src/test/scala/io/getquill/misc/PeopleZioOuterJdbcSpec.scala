package io.getquill.misc

import io.getquill.base.Spec
import io.getquill.{Literal, PostgresZioJdbcContext}
import zio.{Runtime, Unsafe, ZEnvironment}

class PeopleZioOuterJdbcSpec extends Spec {
  val testContext = new PostgresZioJdbcContext(Literal)
  import testContext._
  case class Person(name: String, age: Int)

  def ds = io.getquill.postgres.pool

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
    val a    = testContext.translate(q)
    val exec = a.provideEnvironment(ZEnvironment(ds))
    println(Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(exec).getOrThrow()
    })
  }
}
