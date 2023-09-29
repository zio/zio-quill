package io.getquill.examples.other

import io.getquill._
import io.getquill.util.LoadConfig
import zio.Console.printLine
import zio.{ZEnvironment, ZIOAppDefault}

object ZioAppDataSource extends ZIOAppDefault {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  def dataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  override def run = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    MyPostgresContext
      .run(people)
      .provideEnvironment(ZEnvironment(dataSource))
      .tap(result => printLine(result.toString))
      .exitCode
  }
}
