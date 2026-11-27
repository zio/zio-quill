package zio.quill.examples.other

import zio.quill._
import zio.quill.util.LoadConfig
import zio.{ZIOAppDefault, ZLayer}
import zio.Console.printLine

import javax.sql.DataSource

object ZioAppManual extends ZIOAppDefault {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)
  lazy val ds: DataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  override def run = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    MyPostgresContext
      .run(people)
      .tap(result => printLine(result.toString))
      .provide(ZLayer.succeed(ds))
      .exitCode
  }
}
