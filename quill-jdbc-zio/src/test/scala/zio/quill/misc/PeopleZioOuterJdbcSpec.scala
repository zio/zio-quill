package zio.quill.misc

import zio.quill.base.Spec
import zio.quill.{Literal, PostgresZioJdbcContext}
import zio.{Runtime, Unsafe, ZEnvironment}

class PeopleZioOuterJdbcSpec extends Spec {
  val testContext = new PostgresZioJdbcContext(Literal)
  import testContext._
  case class Person(name: String, age: Int)

  def ds = zio.quill.postgres.pool

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
    println(testContext.translate(q))
  }
}
