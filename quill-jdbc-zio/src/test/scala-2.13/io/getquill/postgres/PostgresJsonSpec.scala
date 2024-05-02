package io.getquill.postgres

import io.getquill.{JsonValue, JsonbValue, ZioSpec}
import org.scalatest.BeforeAndAfterEach
import zio.Chunk
import zio.json.ast.Json
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

class PostgresJsonSpec extends ZioSpec with BeforeAndAfterEach {
  val context = testContext
  import testContext._

  case class PersonJson(name: String, age: Int)
  case class PersonJsonb(name: String, age: Int)

  case class JsonEntity(name: String, value: JsonValue[PersonJson])
  case class JsonbEntity(name: String, value: JsonbValue[PersonJsonb])

  case class JsonOptEntity(name: String, value: Option[JsonValue[PersonJson]])
  case class JsonbOptEntity(name: String, value: Option[JsonbValue[PersonJsonb]])

  val jsonJoe    = JsonValue(PersonJson("Joe", 123))
  val jsonValue  = JsonEntity("JoeEntity", jsonJoe)
  val jsonbJoe   = JsonbValue(PersonJsonb("Joe", 123))
  val jsonbValue = JsonbEntity("JoeEntity", jsonbJoe)

  case class JsonAstEntity(name: String, value: JsonValue[Json])
  case class JsonbAstEntity(name: String, value: JsonbValue[Json])

  case class JsonAstOptEntity(name: String, value: Option[JsonValue[Json]])
  case class JsonbAstOptEntity(name: String, value: Option[JsonbValue[Json]])

  implicit val personJsonEncoder: JsonEncoder[PersonJson] = DeriveJsonEncoder.gen[PersonJson]
  implicit val personJsonDecoder: JsonDecoder[PersonJson] = DeriveJsonDecoder.gen[PersonJson]

  implicit val personJsonbEncoder: JsonEncoder[PersonJsonb] = DeriveJsonEncoder.gen[PersonJsonb]
  implicit val personJsonbDecoder: JsonDecoder[PersonJsonb] = DeriveJsonDecoder.gen[PersonJsonb]

  override def beforeEach = {
    super.beforeEach()
    testContext.run(quote(query[JsonbEntity].delete)).runSyncUnsafe()
    testContext.run(quote(query[JsonEntity].delete)).runSyncUnsafe()
    ()
  }

  "encodes and decodes json entity" - {
    "json" in {
      testContext.run(query[JsonEntity].insertValue(lift(jsonValue))).runSyncUnsafe()
      val inserted = testContext.run(query[JsonEntity]).runSyncUnsafe().head
      inserted mustEqual jsonValue
    }

    "jsonb" in {
      testContext.run(query[JsonbEntity].insertValue(lift(jsonbValue))).runSyncUnsafe()
      val inserted = testContext.run(query[JsonbEntity]).runSyncUnsafe().head
      inserted mustEqual jsonbValue
    }
  }

  "encodes and decodes optional json entity" - {
    val jsonOptQuery  = quote(querySchema[JsonOptEntity]("JsonEntity"))
    val jsonbOptQuery = quote(querySchema[JsonbOptEntity]("JsonEntity"))

    "some json" in {
      val value = JsonOptEntity("JoeEntity", Some(jsonJoe))

      testContext.run(jsonOptQuery.insertValue(lift(value))).runSyncUnsafe()
      val inserted = testContext.run(jsonOptQuery).runSyncUnsafe().head
      inserted mustEqual value
    }

    "some jsonb" in {
      val value = JsonbOptEntity("JoeEntity", Some(jsonbJoe))

      testContext.run(jsonbOptQuery.insertValue(lift(value))).runSyncUnsafe()
      val inserted = testContext.run(jsonbOptQuery).runSyncUnsafe().head
      inserted mustEqual value
    }

    "none json" in {
      val value = JsonOptEntity("JoeEntity", None)

      testContext.run(jsonOptQuery.insertValue(lift(value))).runSyncUnsafe()
      val inserted = testContext.run(jsonOptQuery).runSyncUnsafe().head
      inserted mustEqual value
    }

    "none jsonb" in {
      val value = JsonbOptEntity("JoeEntity", None)

      testContext.run(jsonbOptQuery.insertValue(lift(value))).runSyncUnsafe()
      val inserted = testContext.run(jsonbOptQuery).runSyncUnsafe().head
      inserted mustEqual value
    }
  }

  "encodes and decodes json ast" - {
    val jsonJoe       = Json.Obj(Chunk("age" -> Json.Num(123), "name" -> Json.Str("Joe")))
    val jsonAstQuery  = quote(querySchema[JsonAstEntity]("JsonEntity"))
    val jsonbAstQuery = quote(querySchema[JsonbAstEntity]("JsonbEntity"))

    val jsonAstValue  = JsonAstEntity("JoeEntity", JsonValue(jsonJoe))
    val jsonbAstValue = JsonbAstEntity("JoeEntity", JsonbValue(jsonJoe))

    "json" in {
      testContext.run(jsonAstQuery.insertValue(lift(jsonAstValue))).runSyncUnsafe()
      val inserted = testContext.run(jsonAstQuery).runSyncUnsafe().head
      inserted mustEqual jsonAstValue
    }

    "jsonb" in {
      testContext.run(jsonbAstQuery.insertValue(lift(jsonbAstValue))).runSyncUnsafe()
      val inserted = testContext.run(jsonbAstQuery).runSyncUnsafe().head
      inserted mustEqual jsonbAstValue
    }
  }

  "encodes and decodes optional json ast" - {
    val jsonJoe          = Json.Obj(Chunk("age" -> Json.Num(123), "name" -> Json.Str("Joe")))
    val jsonAstOptQuery  = quote(querySchema[JsonAstOptEntity]("JsonEntity"))
    val jsonbAstOptQuery = quote(querySchema[JsonbAstOptEntity]("JsonbEntity"))

    val jsonbAstValue = JsonbAstEntity("JoeEntity", JsonbValue(jsonJoe))

    "some json" in {
      val value = JsonAstOptEntity("JoeEntity", Some(JsonValue(jsonJoe)))
      testContext.run(jsonAstOptQuery.insertValue(lift(value))).runSyncUnsafe()
      val inserted = testContext.run(jsonAstOptQuery).runSyncUnsafe().head
      inserted mustEqual value
    }

    "some jsonb" in {
      val value = JsonbAstOptEntity("JoeEntity", Some(JsonbValue(jsonJoe)))
      testContext.run(jsonbAstOptQuery.insertValue(lift(value))).runSyncUnsafe()
      val inserted = testContext.run(jsonbAstOptQuery).runSyncUnsafe().head
      inserted mustEqual value
    }

    "none json" in {
      val value = JsonAstOptEntity("JoeEntity", None)
      testContext.run(jsonAstOptQuery.insertValue(lift(value))).runSyncUnsafe()
      val inserted = testContext.run(jsonAstOptQuery).runSyncUnsafe().head
      inserted mustEqual value
    }

    "none jsonb" in {
      val value = JsonbAstOptEntity("JoeEntity", None)
      testContext.run(jsonbAstOptQuery.insertValue(lift(value))).runSyncUnsafe()
      val inserted = testContext.run(jsonbAstOptQuery).runSyncUnsafe().head
      inserted mustEqual value
    }

  }

}
