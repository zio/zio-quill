package io.getquill.examples

import io.getquill._
import io.getquill.util.LoadConfig
import zio.{ App, ExitCode, Task, URIO, ZLayer, ZManaged }
import zio.console.putStrLn

object ZioApp extends App {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  import MyPostgresContext._

  case class Person(name: String, age: Int)

  val zioConn =
    ZLayer.fromManaged(for {
      ds <- ZManaged.fromAutoCloseable(Task(JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource))
      conn <- ZManaged.fromAutoCloseable(Task(ds.getConnection))
    } yield conn)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val people = quote {
      query[Person].filter(p => p.name == "Alex")
    }
    MyPostgresContext.run(people)
      .tap(result => putStrLn(result.toString))
      .provideCustomLayer(zioConn).exitCode
  }

  // NOTE: provideCustomLayer argument whatever zio does not
}
