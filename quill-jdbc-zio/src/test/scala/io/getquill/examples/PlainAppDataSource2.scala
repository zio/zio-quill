package io.getquill.examples

import com.zaxxer.hikari.HikariDataSource
import io.getquill.context.ZioJdbc.{ QConnection, QDataSource }
import io.getquill.util.LoadConfig
import io.getquill.{ JdbcContextConfig, Literal, PostgresZioJdbcContext }
import zio.{ Runtime, Task, ZLayer }
import zio.blocking.Blocking
import zio.console.putStrLn

object PlainAppDataSource2 {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  def config = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  val zioConn: ZLayer[Blocking, Throwable, QConnection] = {
    (for {
      hikariConfig <- Task(config).toManaged_
      ds <- QDataSource.Managed.fromDataSource(new HikariDataSource(hikariConfig))
    } yield ds).toLayerMany >>> QDataSource.toConnection
  }

  def main(args: Array[String]): Unit = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    val qzio =
      MyPostgresContext.run(people)
        .tap(result => putStrLn(result.toString))
        .provideCustomLayer(zioConn)

    Runtime.default.unsafeRun(qzio)
    ()
  }
}
