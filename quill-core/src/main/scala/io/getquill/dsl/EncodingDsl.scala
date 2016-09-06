package io.getquill.dsl

import scala.language.higherKinds
import scala.annotation.compileTimeOnly
import io.getquill.quotation.NonQuotedException
import scala.language.experimental.macros

trait LowPriorityImplicits {
  this: EncodingDsl =>

  implicit def materializeEncoder[T]: Encoder[T] = macro EncodingDslMacro.materializeEncoder[T]
  implicit def materializeDecoder[T]: Decoder[T] = macro EncodingDslMacro.materializeDecoder[T]
}

trait EncodingDsl extends LowPriorityImplicits {
  this: CoreDsl =>

  type PrepareRow
  type ResultRow

  trait Decoder[T] {
    def apply(index: Int, row: ResultRow): T
  }

  def Decoder[T](f: (Int, ResultRow) => T) =
    new Decoder[T] {
      override def apply(index: Int, row: ResultRow) =
        f(index, row)
    }

  trait Encoder[T] {
    def apply(index: Int, value: T, row: PrepareRow): PrepareRow
  }

  def Encoder[T](f: (Int, T, PrepareRow) => PrepareRow) =
    new Encoder[T] {
      override def apply(index: Int, value: T, row: PrepareRow) =
        f(index, value, row)
    }

  /* ************************************************************************** */

  def lift[T](v: T): T = macro EncodingDslMacro.lift[T]

  @compileTimeOnly(NonQuotedException.message)
  def liftScalar[T](v: T)(implicit e: Encoder[T]): T = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def liftCaseClass[T](v: T): T = NonQuotedException()

  /* ************************************************************************** */

  def liftQuery[U[_] <: Traversable[_], T](v: U[T]): Query[T] = macro EncodingDslMacro.liftQuery[T]

  @compileTimeOnly(NonQuotedException.message)
  def liftQueryScalar[U[_] <: Traversable[_], T](v: U[T])(implicit e: Encoder[T]): Query[T] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def liftQueryCaseClass[U[_] <: Traversable[_], T](v: U[T]): Query[T] = NonQuotedException()

  /* ************************************************************************** */

  type MappedEncoding[I, O] = io.getquill.MappedEncoding[I, O]
  val MappedEncoding = io.getquill.MappedEncoding

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    mappedDecoderImpl[I, O]

  protected def mappedDecoderImpl[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    new Decoder[O] {
      def apply(index: Int, row: ResultRow) =
        mapped.f(decoder(index, row))
    }

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I] =
    mappedEncoderImpl[I, O]

  protected def mappedEncoderImpl[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I] =
    new Encoder[I] {
      def apply(index: Int, value: I, row: PrepareRow) =
        encoder(index, mapped.f(value), row)
    }
}
