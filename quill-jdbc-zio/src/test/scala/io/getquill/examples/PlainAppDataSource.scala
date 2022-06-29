package io.getquill.examples

import com.zaxxer.hikari.HikariDataSource
import io.getquill.context.ZioJdbc._
import io.getquill.util.LoadConfig
import io.getquill.{ JdbcContextConfig, Literal, PostgresZioJdbcContext }
import zio.Console.printLine
import zio.{ Runtime, Unsafe }

object PlainAppDataSource {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  def config = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  val zioDS = DataSourceLayer.fromDataSource(new HikariDataSource(config))

  def main(args: Array[String]): Unit = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    val qzio =
      MyPostgresContext.run(people)
        .tap(result => printLine(result.toString))
        .provide(zioDS)

    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe.run(qzio).getOrThrow()
    }
    ()
  }
}
