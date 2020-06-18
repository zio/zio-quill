package io.getquill.context.finagle.postgres

import java.nio.charset.Charset
import java.time.{ LocalDate, LocalDateTime, ZoneId }
import java.util.{ Date, UUID }

import com.twitter.finagle.postgres.values.ValueDecoder
import com.twitter.util.Return
import com.twitter.util.Throw
import com.twitter.util.Try
import io.getquill.FinaglePostgresContext
import io.getquill.util.Messages.fail
import io.netty.buffer.ByteBuf

trait FinaglePostgresDecoders {
  this: FinaglePostgresContext[_] =>

  import ValueDecoder._

  type Decoder[T] = FinaglePostgresDecoder[T]

  case class FinaglePostgresDecoder[T](
    vd:      ValueDecoder[T],
    default: Throwable => T  = (e: Throwable) => fail(e.getMessage)
  ) extends BaseDecoder[T] {
    override def apply(index: Index, row: ResultRow): T =
      row.getTry[T](index)(vd) match {
        case Return(r) => r
        case Throw(e)  => default(e)
      }

    def orElse[U](f: U => T)(implicit vdu: ValueDecoder[U]): FinaglePostgresDecoder[T] = {
      val mappedVd = vdu.map[T](f)
      FinaglePostgresDecoder[T](
        new ValueDecoder[T] {
          def decodeText(recv: String, text: String): Try[T] = {
            val t = vd.decodeText(recv, text)
            if (t.isReturn) t
            else mappedVd.decodeText(recv, text)
          }
          def decodeBinary(recv: String, bytes: ByteBuf, charset: Charset): Try[T] = {
            val t = vd.decodeBinary(recv, bytes, charset)
            if (t.isReturn) t
            else mappedVd.decodeBinary(recv, bytes, charset)
          }
        }
      )
    }
  }

  implicit def decoderDirectly[T](implicit vd: ValueDecoder[T]): Decoder[T] = FinaglePostgresDecoder(vd)
  def decoderMapped[U, T](f: U => T)(implicit vd: ValueDecoder[U]): Decoder[T] = FinaglePostgresDecoder(vd.map[T](f))

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] =
    FinaglePostgresDecoder[Option[T]](
      new ValueDecoder[Option[T]] {
        def decodeText(recv: String, text: String): Try[Option[T]] = Return(d.vd.decodeText(recv, text).toOption)
        def decodeBinary(recv: String, bytes: ByteBuf, charset: Charset): Try[Option[T]] = Return(d.vd.decodeBinary(recv, bytes, charset).toOption)
      },
      _ => None
    )

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    decoderMapped[I, O](mapped.f)(d.vd)

  implicit val stringDecoder: Decoder[String] = decoderDirectly[String]
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoderDirectly[BigDecimal]
  implicit val booleanDecoder: Decoder[Boolean] = decoderDirectly[Boolean]
  implicit val shortDecoder: Decoder[Short] = decoderDirectly[Short]
  implicit val byteDecoder: Decoder[Byte] = decoderMapped[Short, Byte](_.toByte)
  implicit val intDecoder: Decoder[Int] = decoderDirectly[Int].orElse[Long](_.toInt)
  implicit val longDecoder: Decoder[Long] = decoderDirectly[Long].orElse[Int](_.toLong)
  implicit val floatDecoder: Decoder[Float] = decoderDirectly[Float].orElse[Double](_.toFloat)
  implicit val doubleDecoder: Decoder[Double] = decoderDirectly[Double]
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoderDirectly[Array[Byte]]
  implicit val dateDecoder: Decoder[Date] = decoderMapped[LocalDateTime, Date](d => Date.from(d.atZone(ZoneId.systemDefault()).toInstant))
  implicit val localDateDecoder: Decoder[LocalDate] = decoderDirectly[LocalDate].orElse[LocalDateTime](_.toLocalDate)
  implicit val localDateTimeDecoder: Decoder[LocalDateTime] = decoderDirectly[LocalDateTime].orElse[LocalDate](_.atStartOfDay)
  implicit val uuidDecoder: Decoder[UUID] = decoderDirectly[UUID]
}
