package io.getquill.examples

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import io.getquill.util.LoadConfig
import io.getquill.{ JdbcContextConfig, Literal, PostgresZioJdbcContext }
import zio.Console.printLine
import zio.{ Runtime, ZIO, ZLayer }

import javax.sql.DataSource

object PlainAppDataSource2 {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  def hikariConfig = new HikariConfig(JdbcContextConfig(LoadConfig("testPostgresDB")).configProperties)
  def hikariDataSource = new HikariDataSource(hikariConfig)

  val zioDS: ZLayer[Any, Throwable, DataSource] =
    ZLayer(ZIO.attempt(hikariDataSource))

  def main(args: Array[String]): Unit = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    val qzio =
      MyPostgresContext.run(people)
        .tap(result => printLine(result.toString))
        .provide(zioDS)

    Runtime.default.unsafeRun(qzio)
    ()
  }
}
