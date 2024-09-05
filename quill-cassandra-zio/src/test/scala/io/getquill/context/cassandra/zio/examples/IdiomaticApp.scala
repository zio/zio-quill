package io.getquill.context.cassandra.zio.examples

import io.getquill._
import io.getquill.cassandrazio.Quill
import zio.Console.printLine
import zio.{ZIO, ZIOAppDefault, ZLayer}

object IdiomaticApp extends ZIOAppDefault {

  case class Person(name: String, age: Int)

  case class QueryService(quill: Quill.Cassandra[Literal]) {
    import quill._
    def people = quote {
      query[Person]
    }

    def peopleByName = quote { (name: String) =>
      people.filter(p => p.name == name).allowFiltering
    }
  }
  object QueryService {
    def live: ZLayer[Quill.Cassandra[Literal], Nothing, QueryService] =
      ZLayer.fromFunction(QueryService(_))
  }

  case class DataService(queryService: QueryService) {
    import queryService.quill._
    import queryService.quill
    def getPeople(): ZIO[Any, Throwable, List[Person]] = quill.run(queryService.people)
    def getPeopleByName(name: String): ZIO[Any, Throwable, List[Person]] =
      quill.run(queryService.peopleByName(lift(name)))
  }

  object DataService {
    def getPeople(): ZIO[DataService, Throwable, List[Person]] =
      ZIO.serviceWithZIO[DataService](_.getPeople())
    def getPeopleByName(name: String): ZIO[DataService, Throwable, List[Person]] =
      ZIO.serviceWithZIO[DataService](_.getPeopleByName(name))

    def live: ZLayer[QueryService, Nothing, DataService] =
      ZLayer.fromFunction(DataService(_))
  }

  override def run =
    (for {
      people <- DataService.getPeople()
      _      <- printLine(s"People: ${people}")
      joes   <- DataService.getPeopleByName("Joe")
      _      <- printLine(s"Joes: ${joes}")
    } yield ())
      .provide(
        Quill.CassandraZioSession.fromPrefix("testStreamDB"),
        Quill.Cassandra.fromNamingStrategy(Literal),
        QueryService.live,
        DataService.live
      )
      .tapError(e => ZIO.succeed(println(s"Error Occurred: ${e}")))
      .exitCode
}
