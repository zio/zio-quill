package io.getquill.dsl

import scala.language.experimental.macros
import io.getquill.quotation.NonQuotedException
import io.getquill.EntityQuery

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

  object extras extends LowPriorityExtras {
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
