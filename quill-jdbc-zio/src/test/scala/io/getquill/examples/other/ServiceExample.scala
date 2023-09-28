package io.getquill.examples.other

import io.getquill.jdbczio.Quill
import io.getquill.{Literal, PostgresZioJdbcContext}
import zio.Console._
import zio.{ZIO, ZIOAppDefault, ZLayer}

import java.sql.SQLException
import javax.sql.DataSource
import zio.{ ExitCode, URIO }

object ServiceExample extends ZIOAppDefault {
  import DBModel._

  override def run: URIO[Any,ExitCode] =
    runApp.provide(DBManager.live).exitCode

  def runApp: ZIO[DBManager.Service,Throwable,Unit] =
    for {
      _    <- DBManager.deleteJoes
      _    <- DBManager.persist(Person("Joe", 123))
      joes <- DBManager.retrieveJoes
      _    <- printLine(s"Joes: ${joes}")
    } yield ()
}

object DBModel {
  final case class Person(name: String, age: Int)
}

object DBManager {
  import DBModel._

  val ctx = new PostgresZioJdbcContext(Literal)
  import ctx._
  val zioDS: ZLayer[Any, Throwable, DataSource] = Quill.DataSource.fromPrefix("testPostgresDB")

  trait Service {
    def persist(person: Person): ZIO[DataSource, SQLException, Long]
    def retrieveJoes: ZIO[DataSource, SQLException, List[Person]]
    def deleteJoes: ZIO[DataSource, SQLException, Long]
  }

  val live: ZLayer[Any, Nothing, DBManager.Service] = ZLayer.succeed(new Service {
    def persist(person: Person): ZIO[DataSource, SQLException, Long] =
      ctx.run(quote(query[Person].insertValue(lift(person))))

    def retrieveJoes: ZIO[DataSource, SQLException, List[Person]] =
      ctx.run(quote(query[Person].filter(p => p.name == "Joe")))

    def deleteJoes: ZIO[DataSource, SQLException, Long] =
      ctx.run(quote(query[Person].filter(p => p.name == "Joe").delete))
  })

  def persist(person: Person): ZIO[DBManager.Service, Throwable, Long] =
    ZIO.serviceWithZIO(hasService => hasService.persist(person).provideSomeLayer(zioDS))

  def retrieveJoes: ZIO[DBManager.Service, Throwable, List[Person]] =
    ZIO.serviceWithZIO(service => service.retrieveJoes.provideSomeLayer(zioDS))

  def deleteJoes: ZIO[DBManager.Service, Throwable, Long] =
    ZIO.serviceWithZIO(service => service.deleteJoes.provideSomeLayer(zioDS))
}
