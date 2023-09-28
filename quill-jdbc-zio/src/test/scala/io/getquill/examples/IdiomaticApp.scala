package io.getquill.examples

import io.getquill._
import io.getquill.jdbczio.Quill
import zio.Console.printLine
import zio._

import java.sql.SQLException
import javax.sql.DataSource

object IdiomaticApp extends ZIOAppDefault {

  final case class DataService(quill: Quill.Postgres[Literal]) {
    import quill._
    val people: Quoted[EntityQuery[Person]] = quote(query[Person])
    def peopleByName: Quoted[String => EntityQuery[Person]] =
      quote((name: String) => people.filter(p => p.name == name))
  }
  final case class ApplicationLive(dataService: DataService) {
    import dataService.quill._
    import dataService.quill
    def getPeopleByName(name: String): ZIO[Any, SQLException, List[Person]] =
      quill.run(dataService.peopleByName(lift(name)))
    def getAllPeople(): ZIO[Any, SQLException, List[Person]] = quill.run(dataService.people)
  }
  object Application {
    def getPeopleByName(name: String): ZIO[ApplicationLive with ApplicationLive, SQLException, List[Person]] =
      ZIO.serviceWithZIO[ApplicationLive](_.getPeopleByName(name))
    def getAllPeople(): ZIO[ApplicationLive with ApplicationLive, SQLException, List[Person]] =
      ZIO.serviceWithZIO[ApplicationLive](_.getAllPeople())
  }
  final case class Person(name: String, age: Int)

  val dataServiceLive: ZLayer[Quill.Postgres[Literal], Nothing, DataService] = ZLayer.fromFunction(DataService.apply _)
  val applicationLive: ZLayer[DataService, Nothing, ApplicationLive]         = ZLayer.fromFunction(ApplicationLive.apply _)
  val dataSourceLive: ZLayer[Any, Throwable, DataSource]                     = Quill.DataSource.fromPrefix("testPostgresDB")
  val postgresLive: ZLayer[DataSource, Nothing, Quill.Postgres[Literal.type]] =
    Quill.Postgres.fromNamingStrategy(Literal)

  override def run: ZIO[Any, Throwable, Unit] =
    (for {
      joes      <- Application.getPeopleByName("Joe")
      _         <- printLine(joes)
      allPeople <- Application.getAllPeople()
      _         <- printLine(allPeople)
    } yield ()).provide(applicationLive, dataServiceLive, dataSourceLive, postgresLive)
}
