package io.getquill.examples.other

import io.getquill._
import io.getquill.jdbczio.Quill
import zio.Console.printLine
import zio.ZIOAppDefault
import javax.sql.DataSource
import zio.{ExitCode, URIO, ZLayer}

object ZioApp extends ZIOAppDefault {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  final case class Person(name: String, age: Int)

  val zioDS: ZLayer[Any, Throwable, DataSource] = Quill.DataSource.fromPrefix("testPostgresDB")

  override def run: URIO[Any, ExitCode] = {
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
