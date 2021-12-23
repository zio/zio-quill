package io.getquill.examples

import io.getquill._
import io.getquill.context.ZioJdbc.DataSource
import io.getquill.util.LoadConfig
import zio.{App, ExitCode, URIO, ZLayer}
import zio.console.putStrLn

object ZioAppManual extends App {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  final case class Person(name: String, age: Int)
  lazy val ds: DataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    MyPostgresContext.run(people)
      .tap(result => putStrLn(result.toString))
      .provideCustomLayer(ZLayer.succeed(ds))
      .exitCode
  }
}
