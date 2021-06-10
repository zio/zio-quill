package io.getquill.examples

import com.zaxxer.hikari.HikariDataSource
import io.getquill.context.ZioJdbc.DaoLayer
import io.getquill.util.LoadConfig
import io.getquill.{ JdbcContextConfig, Literal, PostgresZioJdbcContext }
import zio.{ Has, Runtime, Task, ZLayer }
import zio.console.putStrLn

import java.io.Closeable
import java.sql.Connection
import javax.sql.DataSource

object PlainAppDataSource2 {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  def dataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  val zioConn: ZLayer[Any, Throwable, Has[Connection]] = {
    (for {
      hikariConfig <- Task(dataSource).toManaged_
      ds <- Task(new HikariDataSource(hikariConfig)).toManaged_
    } yield Has(ds: DataSource with Closeable)).toLayerMany >>> DaoLayer.live
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
