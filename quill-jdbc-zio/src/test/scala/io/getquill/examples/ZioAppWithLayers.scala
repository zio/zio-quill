package io.getquill.examples

import io.getquill._
import io.getquill.context.ZioJdbc.Layers
import zio.blocking.blocking
import zio.console.putStrLn
import zio.{ App, ExitCode, URIO }

object ZioAppWithLayers extends App {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  val zioConn =
    Layers.dataSourceFromPrefix("testPostgresDB") >>> Layers.dataSourceToConnection

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    blocking(
      MyPostgresContext.run(people)
        .tap(result => putStrLn(result.toString))
        .provideCustomLayer(zioConn).exitCode
    )
  }
}
