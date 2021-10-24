package io.getquill.context.cassandra.encoding

import java.time.{ Instant, ZonedDateTime, ZoneId }

import io.getquill.context.cassandra.CassandraContext

trait Encodings extends CassandraMapperConversions with CassandraTypes {
  this: CassandraContext[_] =>

  protected val zoneId = ZoneId.systemDefault

  implicit val encodeJava8ZonedDateTime: MappedEncoding[ZonedDateTime, Instant] = MappedEncoding(zdt => zdt.toInstant)
  implicit val decodeJava8ZonedDateTime: MappedEncoding[Instant, ZonedDateTime] = MappedEncoding(d => ZonedDateTime.ofInstant(d, zoneId))
}
