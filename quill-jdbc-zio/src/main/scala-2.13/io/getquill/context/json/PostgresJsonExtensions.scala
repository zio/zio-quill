package io.getquill.context.json

import io.getquill.{JsonValue, JsonbValue}
import io.getquill.context.jdbc.{Decoders, Encoders}
import zio.json.{JsonDecoder, JsonEncoder}
import zio.json.ast.Json

import java.sql.Types
import scala.reflect.{ClassTag, classTag}

trait PostgresJsonExtensions { this: Encoders with Decoders =>

  implicit final def jsonEntityEncoder[T](implicit jsonEncoder: JsonEncoder[T]): Encoder[JsonValue[T]] =
    entityEncoder[T, JsonValue[T]](_.value)("json", jsonEncoder)
  implicit final def jsonEntityDecoder[T: ClassTag](implicit jsonDecoder: JsonDecoder[T]): Decoder[JsonValue[T]] =
    entityDecoder[T, JsonValue[T]](JsonValue(_))("json", jsonDecoder)
  implicit final def jsonbEntityEncoder[T](implicit jsonEncoder: JsonEncoder[T]): Encoder[JsonbValue[T]] =
    entityEncoder[T, JsonbValue[T]](_.value)("jsonb", jsonEncoder)
  implicit final def jsonbEntityDecoder[T: ClassTag](implicit jsonDecoder: JsonDecoder[T]): Decoder[JsonbValue[T]] =
    entityDecoder[T, JsonbValue[T]](JsonbValue(_))("jsonb", jsonDecoder)

  implicit final val jsonAstEncoder: Encoder[JsonValue[Json]]   = astEncoder(_.value.toString(), "json")
  implicit final val jsonAstDecoder: Decoder[JsonValue[Json]]   = astDecoder(JsonValue(_))
  implicit final val jsonbAstEncoder: Encoder[JsonbValue[Json]] = astEncoder(_.value.toString(), "jsonb")
  implicit final val jsonbAstDecoder: Decoder[JsonbValue[Json]] = astDecoder(JsonbValue(_))

  def astEncoder[Wrapper](valueToString: Wrapper => String, jsonType: String): Encoder[Wrapper] =
    encoder(
      Types.OTHER,
      (index, jsonValue, row) => {
        val obj = new org.postgresql.util.PGobject()
        obj.setType(jsonType)
        val jsonString = valueToString(jsonValue)
        obj.setValue(jsonString)
        row.setObject(index, obj)
      }
    )

  def astDecoder[Wrapper](valueFromString: Json => Wrapper): Decoder[Wrapper] =
    decoder { (index, row, session) =>
      val obj        = row.getObject(index, classOf[org.postgresql.util.PGobject])
      val jsonString = obj.getValue
      Json.decoder.decodeJson(jsonString) match {
        case Right(value) => valueFromString(value)
        case Left(error) =>
          throw new IllegalArgumentException(
            s"Error decoding the Json value '${jsonString}' into a zio.json.ast.Json. Message: ${error}"
          )
      }
    }

  def entityEncoder[JsValue, Wrapper](
    unwrap: Wrapper => JsValue
  )(
    jsonType: String,
    jsonEncoder: JsonEncoder[JsValue]
  ): Encoder[Wrapper] =
    encoder(
      Types.OTHER,
      (index, jsonValue, row) => {
        val obj = new org.postgresql.util.PGobject()
        obj.setType(jsonType)
        val jsonString = jsonEncoder.encodeJson(unwrap(jsonValue), None).toString
        obj.setValue(jsonString)
        row.setObject(index, obj)
      }
    )

  def entityDecoder[JsValue: ClassTag, Wrapper](
    wrap: JsValue => Wrapper
  )(
    jsonType: String,
    jsonDecoder: JsonDecoder[JsValue]
  ): Decoder[Wrapper] =
    decoder { (index, row, session) =>
      val obj        = row.getObject(index, classOf[org.postgresql.util.PGobject])
      val jsonString = obj.getValue
      jsonDecoder.decodeJson(jsonString) match {
        case Right(value) => wrap(value)
        case Left(error) =>
          throw new IllegalArgumentException(
            s"Error decoding the Json value '${jsonString}' into a ${classTag[JsValue]}. Message: ${error}"
          )
      }
    }
}
