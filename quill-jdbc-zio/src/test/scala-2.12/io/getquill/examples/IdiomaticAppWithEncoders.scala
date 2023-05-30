package io.getquill.examples

import io.getquill.{JsonbValue, Literal}
import io.getquill.jdbczio.Quill
import zio.Console.printLine
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}
import zio.{ZIO, ZIOAppDefault, ZLayer}

object IdiomaticAppWithEncoders extends ZIOAppDefault {

  case class Person(name: String, age: Int)
  case class JsonbEntity(name: String, value: JsonbValue[Person])

  implicit val personEncoder: JsonEncoder[Person] = DeriveJsonEncoder.gen[Person]
  implicit val personDecoder: JsonDecoder[Person] = DeriveJsonDecoder.gen[Person]

  case class App(quill: Quill.Postgres[Literal]) {
    import quill._

    val v        = JsonbEntity("JoeEntity", JsonbValue(Person("Joe", 123)))
    val setQuery = quote(query[JsonbEntity].insertValue(lift(v)))
    val getQuery = quote(query[JsonbEntity])
    def get()    = quill.run(getQuery)
    def set()    = quill.run(setQuery)
  }

  object App {
    def get() = ZIO.serviceWithZIO[App](_.get())
    def set() = ZIO.serviceWithZIO[App](_.set())
  }

  val dsLive  = Quill.DataSource.fromPrefix("testPostgresDB")
  val pgLive  = Quill.Postgres.fromNamingStrategy(Literal)
  val appLive = ZLayer.fromFunction(App.apply _)

  override def run =
    (for {
      _   <- App.set()
      ent <- App.get()
      _   <- printLine(ent)
    } yield ()).provide(dsLive, pgLive, appLive)
}
