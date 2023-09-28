package io.getquill.postgres

import io.getquill.{JsonValue, JsonbValue, ZioSpec}
import zio.Chunk
import zio.json.ast.Json
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

class PostgresJsonSpec extends ZioSpec {
  val context = testContext
  import testContext._

  case class PersonJson(name: String, age: Int)
  case class PersonJsonb(name: String, age: Int)

  case class JsonEntity(name: String, value: JsonValue[PersonJson])
  case class JsonbEntity(name: String, value: JsonbValue[PersonJsonb])

  val jsonJoe: JsonValue[PersonJson]    = JsonValue(PersonJson("Joe", 123))
  val jsonValue: JsonEntity             = JsonEntity("JoeEntity", jsonJoe)
  val jsonbJoe: JsonbValue[PersonJsonb] = JsonbValue(PersonJsonb("Joe", 123))
  val jsonbValue: JsonbEntity           = JsonbEntity("JoeEntity", jsonbJoe)

  case class JsonAstEntity(name: String, value: JsonValue[Json])
  case class JsonbAstEntity(name: String, value: JsonbValue[Json])

  implicit val personJsonEncoder: JsonEncoder[PersonJson] = DeriveJsonEncoder.gen[PersonJson]
  implicit val personJsonDecoder: JsonDecoder[PersonJson] = DeriveJsonDecoder.gen[PersonJson]

  implicit val personJsonbEncoder: JsonEncoder[PersonJsonb] = DeriveJsonEncoder.gen[PersonJsonb]
  implicit val personJsonbDecoder: JsonDecoder[PersonJsonb] = DeriveJsonDecoder.gen[PersonJsonb]

  override def beforeAll: Unit = {
    super.beforeAll()
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
}
