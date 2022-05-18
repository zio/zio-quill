package io.getquill.dsl

import scala.language.experimental.macros
import io.getquill.quotation.NonQuotedException
import io.getquill.EntityQuery

import java.util.Date
import java.sql.Timestamp
import java.time.{ Instant, LocalDate, LocalDateTime, LocalTime, OffsetDateTime, OffsetTime, ZonedDateTime }
import scala.annotation.compileTimeOnly

private[getquill] trait QueryDsl {
  dsl: CoreDsl =>

  def query[T]: EntityQuery[T] = macro QueryDslMacro.expandEntity[T]

  @compileTimeOnly(NonQuotedException.message)
  def querySchema[T](entity: String, columns: (T => (Any, String))*): EntityQuery[T] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def impliedQuerySchema[T](entity: String, columns: (T => (Any, String))*): EntityQuery[T] = NonQuotedException()

  implicit class NullableColumnExtensions[A](o: Option[A]) {
    @compileTimeOnly(NonQuotedException.message)
    def getOrNull: A =
      throw new IllegalArgumentException(
        "Cannot use getOrNull outside of database queries since only database value-types (e.g. Int, Double, etc...) can be null."
      )
  }

  object extras extends LowPriorityExtras with TemporalExtras {
    implicit class NumericOptionOps[A: Numeric](a: Option[A]) {
      def ===[B: Numeric](b: Option[B]): Boolean = a.exists(av => b.exists(bv => av == bv))
      def ===[B: Numeric](b: B): Boolean = a.exists(av => av == b)
      def =!=[B: Numeric](b: Option[B]): Boolean = a.exists(av => b.exists(bv => av != bv))
      def =!=[B: Numeric](b: B): Boolean = a.exists(av => av != b)
    }
    implicit class NumericRegOps[A: Numeric](a: A) {
      def ===[B: Numeric](b: Option[B]): Boolean = b.exists(bv => bv == a)
      def ===[B: Numeric](b: B): Boolean = a == b
      def =!=[B: Numeric](b: Option[B]): Boolean = b.exists(bv => bv != a)
      def =!=[B: Numeric](b: B): Boolean = a != b
    }
  }

  trait TemporalExtras {
    implicit class DateOps(a: Date) {
      def ===(b: Option[Date]): Boolean = b.exists(bv => bv.getTime == a.getTime)
      def ===(b: Date): Boolean = a.getTime == b.getTime
      def =!=(b: Option[Date]): Boolean = b.exists(bv => bv.getTime != a.getTime)
      def =!=(b: Date): Boolean = a.getTime != b.getTime
      def >(b: Date): Boolean = a.getTime > b.getTime
      def >=(b: Date): Boolean = a.getTime >= b.getTime
      def <(b: Date): Boolean = a.getTime < b.getTime
      def <=(b: Date): Boolean = a.getTime <= b.getTime
    }
    implicit class TimestampOps(a: Timestamp) {
      def ===(b: Option[Timestamp]): Boolean = b.exists(bv => bv.getTime == a.getTime)
      def ===(b: Timestamp): Boolean = a.getTime == b.getTime
      def =!=(b: Option[Timestamp]): Boolean = b.exists(bv => bv.getTime != a.getTime)
      def =!=(b: Timestamp): Boolean = a.getTime != b.getTime
      def >(b: Timestamp): Boolean = a.getTime > b.getTime
      def >=(b: Timestamp): Boolean = a.getTime >= b.getTime
      def <(b: Timestamp): Boolean = a.getTime < b.getTime
      def <=(b: Timestamp): Boolean = a.getTime <= b.getTime
    }
    implicit class InstantOps(a: Instant) {
      def ===(b: Option[Instant]): Boolean = b.exists(bv => bv.equals(a))
      def ===(b: Instant): Boolean = a.equals(b)
      def =!=(b: Option[Instant]): Boolean = b.exists(bv => !bv.equals(a))
      def =!=(b: Instant): Boolean = !a.equals(b)
      def >(b: Instant): Boolean = a.isAfter(b)
      def >=(b: Instant): Boolean = a.equals(b) || a.isAfter(b)
      def <(b: Instant): Boolean = a.isBefore(b)
      def <=(b: Instant): Boolean = a.equals(b) || a.isBefore(b)
    }
    implicit class LocalDateOps(a: LocalDate) {
      def ===(b: Option[LocalDate]): Boolean = b.exists(bv => bv.equals(a))
      def ===(b: LocalDate): Boolean = a.equals(b)
      def =!=(b: Option[LocalDate]): Boolean = b.exists(bv => !bv.equals(a))
      def =!=(b: LocalDate): Boolean = !a.equals(b)
      def >(b: LocalDate): Boolean = a.isAfter(b)
      def >=(b: LocalDate): Boolean = a.equals(b) || a.isAfter(b)
      def <(b: LocalDate): Boolean = a.isBefore(b)
      def <=(b: LocalDate): Boolean = a.equals(b) || a.isBefore(b)
    }
    implicit class LocalDateTimeOps(a: LocalDateTime) {
      def ===(b: Option[LocalDateTime]): Boolean = b.exists(bv => bv.equals(a))
      def ===(b: LocalDateTime): Boolean = a.equals(b)
      def =!=(b: Option[LocalDateTime]): Boolean = b.exists(bv => !bv.equals(a))
      def =!=(b: LocalDateTime): Boolean = !a.equals(b)
      def >(b: LocalDateTime): Boolean = a.isAfter(b)
      def >=(b: LocalDateTime): Boolean = a.equals(b) || a.isAfter(b)
      def <(b: LocalDateTime): Boolean = a.isBefore(b)
      def <=(b: LocalDateTime): Boolean = a.equals(b) || a.isBefore(b)
    }
    implicit class LocalTimeOps(a: LocalTime) {
      def ===(b: Option[LocalTime]): Boolean = b.exists(bv => bv.equals(a))
      def ===(b: LocalTime): Boolean = a.equals(b)
      def =!=(b: Option[LocalTime]): Boolean = b.exists(bv => !bv.equals(a))
      def =!=(b: LocalTime): Boolean = !a.equals(b)
      def >(b: LocalTime): Boolean = a.isAfter(b)
      def >=(b: LocalTime): Boolean = a.equals(b) || a.isAfter(b)
      def <(b: LocalTime): Boolean = a.isBefore(b)
      def <=(b: LocalTime): Boolean = a.equals(b) || a.isBefore(b)
    }
    implicit class OffsetTimeOps(a: OffsetTime) {
      def ===(b: Option[OffsetTime]): Boolean = b.exists(bv => bv.equals(a))
      def ===(b: OffsetTime): Boolean = a.equals(b)
      def =!=(b: Option[OffsetTime]): Boolean = b.exists(bv => !bv.equals(a))
      def =!=(b: OffsetTime): Boolean = !a.equals(b)
      def >(b: OffsetTime): Boolean = a.isAfter(b)
      def >=(b: OffsetTime): Boolean = a.equals(b) || a.isAfter(b)
      def <(b: OffsetTime): Boolean = a.isBefore(b)
      def <=(b: OffsetTime): Boolean = a.equals(b) || a.isBefore(b)
    }
    implicit class OffsetDateTimeOps(a: OffsetDateTime) {
      def ===(b: Option[OffsetDateTime]): Boolean = b.exists(bv => bv.equals(a))
      def ===(b: OffsetDateTime): Boolean = a.equals(b)
      def =!=(b: Option[OffsetDateTime]): Boolean = b.exists(bv => !bv.equals(a))
      def =!=(b: OffsetDateTime): Boolean = !a.equals(b)
      def >(b: OffsetDateTime): Boolean = a.isAfter(b)
      def >=(b: OffsetDateTime): Boolean = a.equals(b) || a.isAfter(b)
      def <(b: OffsetDateTime): Boolean = a.isBefore(b)
      def <=(b: OffsetDateTime): Boolean = a.equals(b) || a.isBefore(b)
    }
    implicit class ZonedDateTimeOps(a: ZonedDateTime) {
      def ===(b: Option[ZonedDateTime]): Boolean = b.exists(bv => bv.equals(a))
      def ===(b: ZonedDateTime): Boolean = a.equals(b)
      def =!=(b: Option[ZonedDateTime]): Boolean = b.exists(bv => !bv.equals(a))
      def =!=(b: ZonedDateTime): Boolean = !a.equals(b)
      def >(b: ZonedDateTime): Boolean = a.isAfter(b)
      def >=(b: ZonedDateTime): Boolean = a.equals(b) || a.isAfter(b)
      def <(b: ZonedDateTime): Boolean = a.isBefore(b)
      def <=(b: ZonedDateTime): Boolean = a.equals(b) || a.isBefore(b)
    }
  }

  trait LowPriorityExtras {
    implicit class OptionOps[T](a: Option[T]) {
      def ===(b: Option[T]): Boolean = a.exists(av => b.exists(bv => av == bv))
      def ===(b: T): Boolean = a.exists(av => av == b)
      def =!=(b: Option[T]): Boolean = a.exists(av => b.exists(bv => av != bv))
      def =!=(b: T): Boolean = a.exists(av => av != b)
    }
    implicit class RegOps[T](a: T) {
      def ===(b: Option[T]): Boolean = b.exists(bv => bv == a)
      def ===(b: T): Boolean = a == b
      def =!=(b: Option[T]): Boolean = b.exists(bv => bv != a)
      def =!=(b: T): Boolean = a != b
    }
  }
}
