package io.getquill.examples.other

import io.getquill._
import io.getquill.util.LoadConfig
import zio.Console.printLine
import zio.{ZEnvironment, ZIOAppDefault}
import com.zaxxer.hikari.HikariDataSource
import zio.{ ExitCode, URIO }

object ZioAppDataSource extends ZIOAppDefault {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  final case class Person(name: String, age: Int)

  def dataSource: HikariDataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  override def run: URIO[Any,ExitCode] = {
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
