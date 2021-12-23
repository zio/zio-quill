package io.getquill.examples

import com.zaxxer.hikari.HikariDataSource
import io.getquill.context.ZioJdbc._
import io.getquill.util.LoadConfig
import io.getquill.{JdbcContextConfig, Literal, PostgresZioJdbcContext}
import zio.console.putStrLn
import zio.{Has, Runtime, TaskLayer}

import javax.sql.DataSource

object PlainAppDataSource {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  def dataSource: HikariDataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  val zioDS: TaskLayer[Has[DataSource]] = DataSourceLayer.fromDataSource(dataSource)

  def main(args: Array[String]): Unit = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    val qzio =
      MyPostgresContext.run(people)
        .tap(result => putStrLn(result.toString))
        .provideCustomLayer(zioDS)

    Runtime.default.unsafeRun(qzio)
    ()
  }
}
