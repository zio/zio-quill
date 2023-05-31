package io.getquill.examples

import io.getquill._
import io.getquill.jdbczio.Quill
import zio.Console.printLine
import zio._

import java.sql.SQLException

object IdiomaticApp extends ZIOAppDefault {

  case class DataService(quill: Quill.Postgres[Literal]) {
    import quill._
    val people       = quote(query[Person])
    def peopleByName = quote((name: String) => people.filter(p => p.name == name))
  }
  case class ApplicationLive(dataService: DataService) {
    import dataService.quill._
    import dataService.quill
    def getPeopleByName(name: String): ZIO[Any, SQLException, List[Person]] =
      quill.run(dataService.peopleByName(lift(name)))
    def getAllPeople(): ZIO[Any, SQLException, List[Person]] = quill.run(dataService.people)
  }
  object Application {
    def getPeopleByName(name: String) =
      ZIO.serviceWithZIO[ApplicationLive](_.getPeopleByName(name))
    def getAllPeople() =
      ZIO.serviceWithZIO[ApplicationLive](_.getAllPeople())
  }
  case class Person(name: String, age: Int)

  val dataServiceLive = ZLayer.fromFunction(DataService.apply _)
  val applicationLive = ZLayer.fromFunction(ApplicationLive.apply _)
  val dataSourceLive  = Quill.DataSource.fromPrefix("testPostgresDB")
  val postgresLive    = Quill.Postgres.fromNamingStrategy(Literal)

  override def run =
    (for {
      joes      <- Application.getPeopleByName("Joe")
      _         <- printLine(joes)
      allPeople <- Application.getAllPeople()
      _         <- printLine(allPeople)
    } yield ()).provide(applicationLive, dataServiceLive, dataSourceLive, postgresLive)
}
