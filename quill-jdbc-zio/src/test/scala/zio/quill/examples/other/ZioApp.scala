package zio.quill.examples.other

import zio.quill._
import zio.quill.jdbczio.Quill
import zio.Console.printLine
import zio.ZIOAppDefault

object ZioApp extends ZIOAppDefault {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  val zioDS = Quill.DataSource.fromPrefix("testPostgresDB")

  override def run = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    MyPostgresContext
      .run(people)
      .tap(result => printLine(result.toString))
      .provide(zioDS)
      .exitCode
  }
}
