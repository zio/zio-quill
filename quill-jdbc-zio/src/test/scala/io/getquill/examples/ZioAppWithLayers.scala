package io.getquill.examples

import io.getquill._
import io.getquill.context.ZioJdbc._
import zio.console.putStrLn
import zio.{ App, ExitCode, URIO }

object ZioAppWithLayers extends App {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  val zioConn =
    QDataSource.fromPrefix("testPostgresDB") >>>
      QDataSource.toConnection

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    MyPostgresContext.run(people)
      .tap(result => putStrLn(result.toString))
      .provideCustomLayer(zioConn).exitCode
  }
}
