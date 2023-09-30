package io.getquill.dsl

trait DateOps {
  implicit final class SqlDateOps(val value: java.sql.Date) extends Ordered[java.sql.Date] {
    def compare(that: java.sql.Date): Int = value.compareTo(that)
  }

  implicit final class SqlTimeOps(val value: java.sql.Time) extends Ordered[java.sql.Time] {
    def compare(that: java.sql.Time): Int = value.compareTo(that)
  }

  implicit final class SqlTimestampOps(val value: java.sql.Timestamp) extends Ordered[java.sql.Timestamp] {
    def compare(that: java.sql.Timestamp): Int = value.compareTo(that)
  }

  implicit final class InstantOps(val value: java.time.Instant) extends Ordered[java.time.Instant] {
    def compare(that: java.time.Instant): Int = value.compareTo(that)
  }

  implicit final class LocalDateOps(val value: java.time.LocalDate) extends Ordered[java.time.LocalDate] {
    def compare(that: java.time.LocalDate): Int = value.compareTo(that)
  }

  implicit final class LocalTimeOps(val value: java.time.LocalTime) extends Ordered[java.time.LocalTime] {
    def compare(that: java.time.LocalTime): Int = value.compareTo(that)
  }

  implicit final class LocalDateTimeOps(val value: java.time.LocalDateTime) extends Ordered[java.time.LocalDateTime] {
    def compare(that: java.time.LocalDateTime): Int = value.compareTo(that)
  }

  implicit final class ZonedDateTimeOps(val value: java.time.ZonedDateTime) extends Ordered[java.time.ZonedDateTime] {
    def compare(that: java.time.ZonedDateTime): Int = value.compareTo(that)
  }

  implicit final class OffsetTimeOps(val value: java.time.OffsetTime) extends Ordered[java.time.OffsetTime] {
    def compare(that: java.time.OffsetTime): Int = value.compareTo(that)
  }

  implicit final class OffsetDateTimeOps(val value: java.time.OffsetDateTime) extends Ordered[java.time.OffsetDateTime] {
    def compare(that: java.time.OffsetDateTime): Int = value.compareTo(that)
  }
}
