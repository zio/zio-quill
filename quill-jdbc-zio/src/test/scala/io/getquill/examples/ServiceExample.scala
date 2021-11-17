package io.getquill.examples

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.{ Literal, PostgresZioJdbcContext }
import zio.{ App, ExitCode, Has, URIO, ZIO, ZLayer }
import zio.console._

import java.sql.SQLException
import javax.sql.DataSource

object ServiceExample extends App {
  import DBModel._

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    runApp.provideCustomLayer(DBManager.live).exitCode
  }

  def runApp =
    for {
      _ <- DBManager.deleteJoes
      _ <- DBManager.persist(Person("Joe", 123))
      joes <- DBManager.retrieveJoes
      _ <- putStrLn(s"Joes: ${joes}")
    } yield ()
}

object DBModel {
  case class Person(name: String, age: Int)
}

object DBManager {
  import DBModel._

  val ctx = new PostgresZioJdbcContext(Literal)
  import ctx._
  type DBManagerEnv = Has[DBManager.Service]
  val zioDS: ZLayer[Any, Throwable, Has[DataSource]] = DataSourceLayer.fromPrefix("testPostgresDB")

  trait Service {
    def persist(person: Person): ZIO[Has[DataSource], SQLException, Long]
    def retrieveJoes: ZIO[Has[DataSource], SQLException, List[Person]]
    def deleteJoes: ZIO[Has[DataSource], SQLException, Long]
  }

  val live: ZLayer[Any, Nothing, DBManagerEnv] = ZLayer.succeed(new Service {
    def persist(person: Person): ZIO[Has[DataSource], SQLException, Long] =
      ctx.run(quote(query[Person].insert(lift(person))))

    def retrieveJoes: ZIO[Has[DataSource], SQLException, List[Person]] =
      ctx.run(quote(query[Person].filter(p => p.name == "Joe")))

    def deleteJoes: ZIO[Has[DataSource], SQLException, Long] =
      ctx.run(quote(query[Person].filter(p => p.name == "Joe").delete))
  })

  def persist(person: Person): ZIO[Has[DBManager.Service], Throwable, Long] =
    ZIO.accessM(hasService => hasService.get.persist(person).provideSomeLayer(zioDS))

  def retrieveJoes: ZIO[Has[DBManager.Service], Throwable, List[Person]] =
    ZIO.accessM(service => service.get.retrieveJoes.provideSomeLayer(zioDS))

  def deleteJoes: ZIO[Has[DBManager.Service], Throwable, Long] =
    ZIO.accessM(service => service.get.deleteJoes.provideSomeLayer(zioDS))
}
