package io.getquill.examples

import io.getquill._
import io.getquill.context.ZioJdbc._
import zio.console.putStrLn
import zio.{ App, ExitCode, URIO }

object ZioApp extends App {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  val zioDS = DataSourceLayer.fromPrefix("testPostgresDB")

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    MyPostgresContext.run(people).onDataSource
      .tap(result => putStrLn(result.toString))
      .provideCustomLayer(zioDS)
      .exitCode
  }
}
