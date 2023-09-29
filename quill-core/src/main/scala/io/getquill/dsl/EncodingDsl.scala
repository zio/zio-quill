package io.getquill.dsl

import io.getquill.quotation.NonQuotedException

import scala.annotation.compileTimeOnly
import scala.language.experimental.macros
import scala.language.higherKinds
import io.getquill.Query

trait LowPriorityImplicits {
  this: EncodingDsl =>

  implicit def anyValEncoder[T <: AnyVal]: Encoder[T] = macro EncodingDslMacro.anyValEncoder[T]

  implicit def anyValDecoder[T <: AnyVal]: Decoder[T] = macro EncodingDslMacro.anyValDecoder[T]
}

trait EncodingDsl extends LowPriorityImplicits {
  this: CoreDsl =>

  type PrepareRow
  type ResultRow
  type Session
  type Index = Int

  // Make sure the signature of this is different then the decoder, otherwise in abstract contexts
  // it can be implicitly summoned up as the decoder and you'll see something like this when doing PrintMac:
  // implicitly[Decoder[Boolean]](nullChecker). If session needs to be included, add a argument to differentiate
  // it from Decoder[T] or use a inner-trait.
  type BaseNullChecker = (Index, ResultRow) => Boolean

  type NullChecker <: BaseNullChecker

  type BaseEncoder[T] = (Index, T, PrepareRow, Session) => PrepareRow

  type Encoder[T] <: BaseEncoder[T]

  type BaseDecoder[T] = (Index, ResultRow, Session) => T

  type Decoder[T] <: BaseDecoder[T]

  /* ************************************************************************** */

  def lift[T](v: T): T = macro EncodingDslMacro.lift[T]

  @compileTimeOnly(NonQuotedException.message)
  def liftScalar[T](v: T)(implicit e: Encoder[T]): T = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def liftCaseClass[T](v: T): T = NonQuotedException()

  /* ************************************************************************** */

  def liftQuery[U[_] <: Iterable[_], T](v: U[T]): Query[T] = macro EncodingDslMacro.liftQuery[T]

  @compileTimeOnly(NonQuotedException.message)
  def liftQueryScalar[U[_] <: Iterable[_], T](v: U[T])(implicit e: Encoder[T]): Query[T] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def liftQueryCaseClass[U[_] <: Iterable[_], T](v: U[T]): Query[T] = NonQuotedException()

  /* ************************************************************************** */

  type MappedEncoding[I, O] = io.getquill.MappedEncoding[I, O]
  val MappedEncoding = io.getquill.MappedEncoding

  implicit def anyValMappedEncoder[I <: AnyVal, O](implicit
    mapped: MappedEncoding[I, O],
    encoder: Encoder[O]
  ): Encoder[I] = mappedEncoder

  implicit def anyValMappedDecoder[I, O <: AnyVal](implicit
    mapped: MappedEncoding[I, O],
    decoder: Decoder[I]
  ): Decoder[O] = mappedDecoder

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I]

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O]

  protected def mappedBaseEncoder[I, O](mapped: MappedEncoding[I, O], encoder: BaseEncoder[O]): BaseEncoder[I] =
    (index, value, row, session) => encoder(index, mapped.f(value), row, session)

  protected def mappedBaseDecoder[I, O](mapped: MappedEncoding[I, O], decoder: BaseDecoder[I]): BaseDecoder[O] =
    (index, row, session) => mapped.f(decoder(index, row, session))

  implicit def stringEncoder: Encoder[String]
  implicit def bigDecimalEncoder: Encoder[BigDecimal]
  implicit def booleanEncoder: Encoder[Boolean]
  implicit def byteEncoder: Encoder[Byte]
  implicit def shortEncoder: Encoder[Short]
  implicit def intEncoder: Encoder[Int]
  implicit def longEncoder: Encoder[Long]
  implicit def doubleEncoder: Encoder[Double]
}
