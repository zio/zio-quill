package io.getquill.examples

import io.getquill._
import io.getquill.util.LoadConfig
import zio.console.putStrLn
import zio.{ App, ExitCode, Has, URIO, ZIO, ZLayer }

import java.sql.Connection

object ZioAppDataSource extends App {

  object MyPostgresContext extends PostgresZioJdbcContext(Literal)
  val Ctx = MyPostgresContext.underlying
  import Ctx._

  case class Person(name: String, age: Int)

  def dataSource = JdbcContextConfig(LoadConfig("testPostgresDB")).dataSource

  val joes =
    for {
      // Bug, it's a problem if you do MyPostgresContext on a quote from a different context?
      age <- ZIO.service[Int]
      results <- Ctx.run {
        query[Person].filter(p => p.name == "Joe" && p.age > lift(age))
      }
    } yield results

  val jims =
    for {
      // Bug, it's a problem if you do MyPostgresContext on a quote from a different context?
      age <- ZIO.service[Int]
      results <- Ctx.run {
        query[Person].filter(p => p.name == "Jims" && p.age > lift(age))
      }
    } yield results

  val joesAndJims = joes *> jims

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {

    for {
      myInt <- ZIO.service[Int]
      tpe = joesAndJims.provideSomeLayer[Has[Connection]](ZLayer.succeed(myInt))
      result <- Ctx.transaction(tpe)
    } yield (result)

    //    MyPostgresContext.run(people)
    //      .provide(Has(dataSource))
    //      .tap(result => putStrLn(result.toString))
    //      .exitCode
    null
  }
}
