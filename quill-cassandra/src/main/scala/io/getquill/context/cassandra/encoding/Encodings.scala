package io.getquill.context.cassandra.encoding

import java.time.{ Instant, LocalDate, ZonedDateTime, ZoneId }
import java.util.Date

import com.datastax.driver.core.{ LocalDate => CasLocalDate }
import io.getquill.context.cassandra.CassandraContext

trait Encodings extends CassandraMapperConversions with CassandraTypes {
  this: CassandraContext[_] =>

  protected val zoneId = ZoneId.systemDefault

  implicit val encodeJava8LocalDate: MappedEncoding[LocalDate, CasLocalDate] = MappedEncoding(ld =>
    CasLocalDate.fromYearMonthDay(ld.getYear, ld.getMonthValue, ld.getDayOfMonth))
  implicit val decodeJava8LocalDate: MappedEncoding[CasLocalDate, LocalDate] = MappedEncoding(ld =>
    LocalDate.of(ld.getYear, ld.getMonth, ld.getDay))

  implicit val encodeJava8Instant: MappedEncoding[Instant, Date] = MappedEncoding(Date.from)
  implicit val decodeJava8Instant: MappedEncoding[Date, Instant] = MappedEncoding(_.toInstant)

  implicit val encodeJava8ZonedDateTime: MappedEncoding[ZonedDateTime, Date] = MappedEncoding(zdt =>
    Date.from(zdt.toInstant))
  implicit val decodeJava8ZonedDateTime: MappedEncoding[Date, ZonedDateTime] = MappedEncoding(d =>
    ZonedDateTime.ofInstant(d.toInstant, zoneId))
}
