package io.getquill.examples

import com.zaxxer.hikari.{ HikariConfig, HikariDataSource }
import io.getquill.util.LoadConfig
import io.getquill.{ JdbcContextConfig, Literal, PostgresZioJdbcContext }
import zio.console.putStrLn
import zio.{ Has, Runtime, Task, ZLayer, ZManaged }

import javax.sql.DataSource

object PlainAppDataSource2 {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  def hikariConfig = new HikariConfig(JdbcContextConfig(LoadConfig("testPostgresDB")).configProperties)

  val zioDS: ZLayer[Any, Throwable, Has[DataSource]] =
    ZManaged.fromAutoCloseable(Task(new HikariDataSource(hikariConfig))).map(h => h: DataSource).toLayer

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
