package io.getquill.context.cassandra.encoding

import java.lang.{ Boolean => JBoolean, Double => JDouble, Float => JFloat, Integer => JInt, Long => JLong }
import java.math.{ BigDecimal => JBigDecimal }
import java.nio.ByteBuffer
import java.util.{ Date, UUID }

import com.datastax.driver.core.{ LocalDate, TypeCodec }
import com.datastax.driver.core.TypeCodec._
import io.getquill.MappedEncoding
import io.getquill.context.cassandra.MappedType

trait CassandraTypes {

  type CassandraType[T] = MappedType[T, T]

  implicit val intMappedType: MappedType[Int, JInt] = MappedType(int2Integer, Integer2int)
  implicit val longMappedType: MappedType[Long, JLong] = MappedType(long2Long, Long2long)
  implicit val floatMappedType: MappedType[Float, JFloat] = MappedType(float2Float, Float2float)
  implicit val doubleMappedType: MappedType[Double, JDouble] = MappedType(double2Double, Double2double)
  implicit val booleanMappedType: MappedType[Boolean, JBoolean] = MappedType(boolean2Boolean, Boolean2boolean)
  implicit val decimalMappedType: MappedType[BigDecimal, JBigDecimal] = MappedType(_.bigDecimal, BigDecimal.apply)
  implicit val byteArrayCassandraType: MappedType[Array[Byte], ByteBuffer] = MappedType(
    ByteBuffer.wrap,
    bb => {
      val b = new Array[Byte](bb.remaining())
      bb.get(b)
      b
    }
  )

  implicit val stringCassandraType: CassandraType[String] = supportedType[String](varchar)
  implicit val uuidCassandraType: CassandraType[UUID] = supportedType[UUID](uuid)
  implicit val dateCassandraType: CassandraType[Date] = supportedType[Date](timestamp)
  implicit val localDateCassandraType: CassandraType[LocalDate] = supportedType[LocalDate](date)

  implicit def mappedEncodingMappedType[I, O](
    implicit
    m1: MappedEncoding[I, O],
    m2: MappedEncoding[O, I]
  ): MappedType[I, O] = MappedType(m1.f, m2.f)

  private def supportedType[T](codec: TypeCodec[T]): CassandraType[T] = MappedType(identity, identity)
}
