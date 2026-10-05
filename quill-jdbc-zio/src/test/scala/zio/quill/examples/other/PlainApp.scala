package zio.quill.examples.other

import zio.quill.jdbczio.Quill
import zio.quill.{Literal, PostgresZioJdbcContext}
import zio.{Runtime, Unsafe}

object PlainApp {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  val zioDS = Quill.DataSource.fromPrefix("testPostgresDB")

  def main(args: Array[String]): Unit = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    val qzio =
      MyPostgresContext
        .run(people)
        .tap(result => zio.ZIO.attempt(println(result.toString)))
        .provideLayer(zioDS)

    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(qzio).getOrThrow()
    }
    ()
  }
}
