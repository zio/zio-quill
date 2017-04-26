package io.getquill.context.cassandra.encoding

import java.lang.{ Boolean => JBoolean, Double => JDouble, Float => JFloat, Integer => JInt, Long => JLong }
import java.math.{ BigDecimal => JBigDecimal }
import java.nio.ByteBuffer
import java.util.{ Date, UUID }

import com.datastax.driver.core.LocalDate
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

  implicit val stringCassandraType: CassandraType[String] = supportedCassandraType[String]
  implicit val uuidCassandraType: CassandraType[UUID] = supportedCassandraType[UUID]
  implicit val dateCassandraType: CassandraType[Date] = supportedCassandraType[Date]
  implicit val localDateCassandraType: CassandraType[LocalDate] = supportedCassandraType[LocalDate]

  implicit def mappedEncodingForSupportedType[T, Cas](
    implicit
    m1:         MappedEncoding[T, Cas],
    m2:         MappedEncoding[Cas, T],
    mappedType: CassandraType[Cas]
  ): MappedType[T, Cas] = MappedType(m1.f, m2.f)

  implicit def mappedEncodingForMappedType[I, O, Cas](
    m1:         MappedEncoding[I, O],
    m2:         MappedEncoding[O, I],
    mappedType: MappedType[O, Cas]
  ): MappedType[I, Cas] = MappedType(m1.f.andThen(mappedType.encode), mappedType.decode.andThen(m2.f))

  def supportedCassandraType[T]: CassandraType[T] = MappedType(identity, identity)
}
