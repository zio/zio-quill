package io.getquill.dsl

import scala.language.higherKinds
import io.getquill.WrappedType
import scala.annotation.compileTimeOnly
import io.getquill.quotation.NonQuotedException
import scala.language.experimental.macros

trait EncodingDsl {
  this: CoreDsl =>

  type PrepareRow
  type ResultRow

  type Decoder[T] = io.getquill.context.Decoder[ResultRow, T]

  trait Encoder[-T] {
    def apply(index: Int, value: T, row: PrepareRow): PrepareRow
  }

  object Encoder {
    def apply[T](f: (Int, T, PrepareRow) => PrepareRow) =
      new Encoder[T] {
        override def apply(index: Int, value: T, row: PrepareRow) =
          f(index, value, row)
      }
  }

  /* ************************************************************************** */

  def lift[T](v: T): T = macro macroz.DslMacro.lift[T]

  @compileTimeOnly(NonQuotedException.message)
  def liftScalar[T](v: T)(implicit e: Encoder[T]): T = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def liftCaseClass[T](v: T): T = NonQuotedException()

  /* ************************************************************************** */

  def liftQuery[U[_] <: Traversable[_], T](v: U[T]): Query[T] = macro macroz.DslMacro.liftQuery[T]

  @compileTimeOnly(NonQuotedException.message)
  def liftQueryScalar[U[_] <: Traversable[_], T](v: U[T])(implicit e: Encoder[T]): Query[T] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def liftQueryCaseClass[U[_] <: Traversable[_], T](v: U[T]): Query[T] = NonQuotedException()

  /* ************************************************************************** */

  type MappedEncoding[I, O] = io.getquill.MappedEncoding[I, O]
  val MappedEncoding = io.getquill.MappedEncoding

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    new Decoder[O] {
      def apply(index: Int, row: ResultRow) =
        mapped.f(decoder(index, row))
    }

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I] =
    new Encoder[I] {
      def apply(index: Int, value: I, row: PrepareRow) =
        encoder(index, mapped.f(value), row)
    }

  implicit def wrappedTypeDecoder[T <: WrappedType] =
    MappedEncoding[T, T#Type](_.value)
}
