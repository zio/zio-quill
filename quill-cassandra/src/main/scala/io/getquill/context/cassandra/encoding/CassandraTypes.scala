package io.getquill.context.cassandra.encoding

import java.lang.{ Boolean => JBoolean, Byte => JByte, Double => JDouble, Float => JFloat, Integer => JInt, Long => JLong, Short => JShort }
import java.math.{ BigDecimal => JBigDecimal }
import java.nio.ByteBuffer
import java.util.{ Date, UUID }
import java.time.{ Instant, LocalDate }

/**
 * `CassandraTypes` contains implicit markers for already supported types by Cassandra.
 * Any of this type can be used in raw encoders/decoders as well as in collections encoding.
 * For custom types please use `MappedEncoding` as in `MappedTypes` trait for example.
 */
trait CassandraTypes extends CassandraMappedTypes {
  implicit val byteCassandraType: CassandraType[JByte] = CassandraType.of[JByte]
  implicit val shortCassandraType: CassandraType[JShort] = CassandraType.of[JShort]
  implicit val integerCassandraType: CassandraType[JInt] = CassandraType.of[JInt]
  implicit val longCassandraType: CassandraType[JLong] = CassandraType.of[JLong]
  implicit val floatCassandraType: CassandraType[JFloat] = CassandraType.of[JFloat]
  implicit val doubleCassandraType: CassandraType[JDouble] = CassandraType.of[JDouble]
  implicit val booleanCassandraType: CassandraType[JBoolean] = CassandraType.of[JBoolean]
  implicit val decimalCassandraType: CassandraType[JBigDecimal] = CassandraType.of[JBigDecimal]
  implicit val stringCassandraType: CassandraType[String] = CassandraType.of[String]
  implicit val byteBufferCassandraType: CassandraType[ByteBuffer] = CassandraType.of[ByteBuffer]
  implicit val uuidCassandraType: CassandraType[UUID] = CassandraType.of[UUID]
  implicit val dateCassandraType: CassandraType[Instant] = CassandraType.of[Instant]
  implicit val localDateCassandraType: CassandraType[LocalDate] = CassandraType.of[LocalDate]
}

/**
 * `MappedTypes` contains implicit `CassandraMapper` for Scala primitive/common types
 * which are not in relation with CassandraTypes but can be represented as ones.
 */
trait CassandraMappedTypes {

  implicit val encodeByte: CassandraMapper[Byte, JByte] = CassandraMapper.simple(byte2Byte)
  implicit val decodeByte: CassandraMapper[JByte, Byte] = CassandraMapper.simple(Byte2byte)

  implicit val encodeShort: CassandraMapper[Short, JShort] = CassandraMapper.simple(short2Short)
  implicit val decodeShort: CassandraMapper[JShort, Short] = CassandraMapper.simple(Short2short)

  implicit val encodeInt: CassandraMapper[Int, JInt] = CassandraMapper.simple(int2Integer)
  implicit val decodeInt: CassandraMapper[JInt, Int] = CassandraMapper.simple(Integer2int)

  implicit val encodeLong: CassandraMapper[Long, JLong] = CassandraMapper.simple(long2Long)
  implicit val decodeLong: CassandraMapper[JLong, Long] = CassandraMapper.simple(Long2long)

  implicit val encodeFloat: CassandraMapper[Float, JFloat] = CassandraMapper.simple(float2Float)
  implicit val decodeFloat: CassandraMapper[JFloat, Float] = CassandraMapper.simple(Float2float)

  implicit val encodeDouble: CassandraMapper[Double, JDouble] = CassandraMapper.simple(double2Double)
  implicit val decodeDouble: CassandraMapper[JDouble, Double] = CassandraMapper.simple(Double2double)

  implicit val encodeBoolean: CassandraMapper[Boolean, JBoolean] = CassandraMapper.simple(boolean2Boolean)
  implicit val decodeBoolean: CassandraMapper[JBoolean, Boolean] = CassandraMapper.simple(Boolean2boolean)

  implicit val encodeBigDecimal: CassandraMapper[BigDecimal, JBigDecimal] = CassandraMapper.simple(_.bigDecimal)
  implicit val decodeBigDecimal: CassandraMapper[JBigDecimal, BigDecimal] = CassandraMapper.simple(BigDecimal.apply)

  implicit val encodeByteArray: CassandraMapper[Array[Byte], ByteBuffer] = CassandraMapper.simple(ByteBuffer.wrap)
  implicit val decodeByteArray: CassandraMapper[ByteBuffer, Array[Byte]] = CassandraMapper.simple(bb => {
    val b = new Array[Byte](bb.remaining())
    bb.get(b)
    b
  })
}
