package io.getquill.examples

import io.getquill.{JsonbValue, Literal}
import io.getquill.jdbczio.Quill
import zio.Console.printLine
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.{ZIO, ZIOAppDefault, ZLayer}
import io.getquill.{ EntityQuery, Quoted }
import java.sql.SQLException
import javax.sql.DataSource

object IdiomaticAppWithEncoders extends ZIOAppDefault {

  final case class Person(name: String, age: Int)
  final case class JsonbEntity(name: String, value: JsonbValue[Person])

  implicit val personEncoder: JsonEncoder[Person] = DeriveJsonEncoder.gen[Person]
  implicit val personDecoder: JsonDecoder[Person] = DeriveJsonDecoder.gen[Person]

  final case class App(quill: Quill.Postgres[Literal]) {
    import quill._

    val v: JsonbEntity        = JsonbEntity("JoeEntity", JsonbValue(Person("Joe", 123)))
    val setQuery = quote(query[JsonbEntity].insertValue(lift(v)))
    val getQuery: Quoted[EntityQuery[JsonbEntity]] = quote(query[JsonbEntity])
    def get(): ZIO[Any,SQLException,List[JsonbEntity]]    = quill.run(getQuery)
    def set()    = quill.run(setQuery)
  }

  object App {
    def get(): ZIO[App with App,SQLException,List[JsonbEntity]] = ZIO.serviceWithZIO[App](_.get())
    def set(): ZIO[App with App,Nothing,Nothing] = ZIO.serviceWithZIO[App](_.set())
  }

  val dsLive: ZLayer[Any,Throwable,DataSource]  = Quill.DataSource.fromPrefix("testPostgresDB")
  val pgLive: ZLayer[DataSource,Nothing,Quill.Postgres[Literal.type]]  = Quill.Postgres.fromNamingStrategy(Literal)
  val appLive: ZLayer[Quill.Postgres[Literal],Nothing,App] = ZLayer.fromFunction(App.apply _)

  override def run: ZIO[Any,Throwable,Unit] =
    (for {
      _   <- App.set()
      ent <- App.get()
      _   <- printLine(ent)
    } yield ()).provide(dsLive, pgLive, appLive)
}
