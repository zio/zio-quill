package io.getquill.examples

import io.getquill.jdbczio.Quill
import io.getquill._
import zio._
import zio.Console.printLine

import java.sql.SQLException

object IdiomaticAppPlain {

  final case class DataService(quill: Quill.Postgres[Literal]) {
    import quill._
    val people: Quoted[EntityQuery[Person]]       = quote(query[Person])
    def peopleByName: Quoted[String => EntityQuery[Person]] = quote((name: String) => people.filter(p => p.name == name))
  }
  final case class ApplicationLive(dataService: DataService) {
    import dataService.quill._
    def getPeopleByName(name: String): ZIO[Any, SQLException, List[Person]] = run(dataService.peopleByName(lift(name)))
    def getAllPeople(): ZIO[Any, SQLException, List[Person]]                = run(dataService.people)
  }
  object Application {
    def getPeopleByName(name: String): ZIO[ApplicationLive with ApplicationLive,SQLException,List[Person]] =
      ZIO.serviceWithZIO[ApplicationLive](_.getPeopleByName(name))
    def getAllPeople(): ZIO[ApplicationLive with ApplicationLive,SQLException,List[Person]] =
      ZIO.serviceWithZIO[ApplicationLive](_.getAllPeople())
  }
  final case class Person(name: String, age: Int)

  def main(args: Array[String]): Unit = {
    val dataServiceLive = ZLayer.fromFunction(DataService.apply _)
    val applicationLive = ZLayer.fromFunction(ApplicationLive.apply _)
    val dataSourceLive  = Quill.DataSource.fromPrefix("testPostgresDB")
    val postgresLive    = Quill.Postgres.fromNamingStrategy(Literal)

    Unsafe.unsafe { implicit u =>
      Runtime.default.unsafe
        .run(
          (for {
            joes      <- Application.getPeopleByName("Joe")
            _         <- printLine(joes)
            allPeople <- Application.getAllPeople()
            _         <- printLine(allPeople)
          } yield ()).provide(applicationLive, dataServiceLive, dataSourceLive, postgresLive)
        )
        .getOrThrow()
    }
    ()
  }
}
