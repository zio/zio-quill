package io.getquill.context.async

import java.time.{ LocalDate, LocalDateTime, ZoneId }
import java.util.{ Date, UUID }

import org.joda.time.{
  LocalDate => JodaLocalDate,
  LocalDateTime => JodaLocalDateTime
}

trait Encoders { this: AsyncContext[_, _, _] =>

  case class AsyncEncoder[T](sqlType: SqlTypes.SqlTypes)(implicit encoder: Encoder[T])
    extends Encoder[T] {
    def apply(index: Int, value: T, row: List[Any]) =
      encoder.apply(index, value, row)
  }

  def encoder[T](sqlType: SqlTypes.SqlTypes): AsyncEncoder[T] =
    encoder(identity[T], sqlType)

  def encoder[T](f: T => Any, sqlType: SqlTypes.SqlTypes): AsyncEncoder[T] =
    AsyncEncoder[T](sqlType)(new Encoder[T] {
      def apply(index: Int, value: T, row: List[Any]) =
        row :+ f(value)
    })

  override protected def mappedEncoderImpl[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    e match {
      case e @ AsyncEncoder(sqlType) =>
        val enc = new Encoder[I] {
          def apply(index: Int, value: I, row: List[Any]) =
            e(index, mapped.f(value), row)
        }
        AsyncEncoder(sqlType)(enc)
    }

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] =
    new Encoder[Option[T]] {
      def apply(index: Int, value: Option[T], row: List[Any]) = {
        value match {
          case None    => nullEncoder(index, null, row)
          case Some(v) => d(index, v, row)
        }
      }
    }

  private[this] val nullEncoder: Encoder[Null] = encoder[Null](SqlTypes.NULL)

  implicit val stringEncoder = encoder[String](SqlTypes.VARCHAR)
  implicit val bigDecimalEncoder = encoder[BigDecimal](SqlTypes.REAL)
  implicit val booleanEncoder = encoder[Boolean](SqlTypes.BOOLEAN)
  implicit val byteEncoder = encoder[Byte](SqlTypes.TINYINT)
  implicit val shortEncoder = encoder[Short](SqlTypes.SMALLINT)
  implicit val intEncoder = encoder[Int](SqlTypes.INTEGER)
  implicit val longEncoder = encoder[Long](SqlTypes.BIGINT)
  implicit val floatEncoder = encoder[Float](SqlTypes.FLOAT)
  implicit val doubleEncoder = encoder[Double](SqlTypes.DOUBLE)
  implicit val byteArrayEncoder =
    encoder[Array[Byte]](SqlTypes.VARBINARY)
  implicit val dateEncoder =
    encoder[Date]({ (value: Date) =>
      new JodaLocalDateTime(value)
    }, SqlTypes.TIMESTAMP)
  implicit val uuidEncoder = encoder[UUID](SqlTypes.UUID)
  implicit val localDateEncoder: AsyncEncoder[LocalDate] =
    encoder[LocalDate]({ (value: LocalDate) =>
      new JodaLocalDate(
        value.getYear,
        value.getMonthValue,
        value.getDayOfMonth
      )
    }, SqlTypes.DATE)
  implicit val localDateTimeEncoder =
    encoder[LocalDateTime]({ (value: LocalDateTime) =>
      new JodaLocalDateTime(
        value.atZone(ZoneId.systemDefault()).toInstant.toEpochMilli
      )
    }, SqlTypes.TIMESTAMP)
}
