package io.getquill.dsl

import scala.language.experimental.macros
import io.getquill.quotation.NonQuotedException
import io.getquill.EntityQuery

import java.time.{OffsetDateTime, ZonedDateTime}
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
    @compileTimeOnly(NonQuotedException.message)
    def filterIfDefined(f: A => Boolean): Boolean = NonQuotedException()
  }

  def max[A](a: A): A                                  = NonQuotedException()
  def min[A](a: A): A                                  = NonQuotedException()
  def count[A](a: A): A                                = NonQuotedException()
  def avg[A](a: A)(implicit n: Numeric[A]): BigDecimal = NonQuotedException()
  def sum[A](a: A)(implicit n: Numeric[A]): A          = NonQuotedException()

  def avg[A](a: Option[A])(implicit n: Numeric[A]): Option[BigDecimal] = NonQuotedException()
  def sum[A](a: Option[A])(implicit n: Numeric[A]): Option[A]          = NonQuotedException()

  object extras extends LowPriorityExtras with DateOps {
    implicit class NumericOptionOps[A: Numeric](a: Option[A]) {
      def ===[B: Numeric](b: Option[B]): Boolean = a.exists(av => b.exists(bv => av == bv))
      def ===[B: Numeric](b: B): Boolean         = a.exists(av => av == b)
      def =!=[B: Numeric](b: Option[B]): Boolean = a.exists(av => b.exists(bv => av != bv))
      def =!=[B: Numeric](b: B): Boolean         = a.exists(av => av != b)
    }
    implicit class NumericRegOps[A: Numeric](a: A) {
      def ===[B: Numeric](b: Option[B]): Boolean = b.exists(bv => bv == a)
      def ===[B: Numeric](b: B): Boolean         = a == b
      def =!=[B: Numeric](b: Option[B]): Boolean = b.exists(bv => bv != a)
      def =!=[B: Numeric](b: B): Boolean         = a != b
    }
  }

  trait LowPriorityExtras {
    implicit class OptionOps[T](a: Option[T]) {
      def ===(b: Option[T]): Boolean = a.exists(av => b.exists(bv => av == bv))
      def ===(b: T): Boolean         = a.exists(av => av == b)
      def =!=(b: Option[T]): Boolean = a.exists(av => b.exists(bv => av != bv))
      def =!=(b: T): Boolean         = a.exists(av => av != b)
    }
    implicit class RegOps[T](a: T) {
      def ===(b: Option[T]): Boolean = b.exists(bv => bv == a)
      def ===(b: T): Boolean         = a == b
      def =!=(b: Option[T]): Boolean = b.exists(bv => bv != a)
      def =!=(b: T): Boolean         = a != b
    }
  }
}
