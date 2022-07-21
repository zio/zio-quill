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

trait GenericEncoder[T, PrepareRow, Session] extends ((Int, T, PrepareRow, Session) => PrepareRow) {
  def apply(i: Int, t: T, row: PrepareRow, session: Session): PrepareRow
}

trait GenericDecoder[ResultRow, Session, T] extends ((Int, ResultRow, Session) => T) {
  def apply(i: Int, rr: ResultRow, s: Session): T
}

trait GenericNullChecker[ResultRow, Session] {
  def apply(columnIndex: Int, resultRow: ResultRow): Boolean
}

trait EncodingDsl extends LowPriorityImplicits {
  this: CoreDsl =>

  type PrepareRow
  type ResultRow
  type Session
  type Index = Int

  type EncoderMethod[T] = (Int, T, PrepareRow, Session) => PrepareRow
  type DecoderMethod[T] = (Int, ResultRow, Session) => T

  // Final Encoder/Decoder classes that Context implementations will use for their actual signatures
  // need to by subtypes GenericEncoder for encoder summoning to work from SqlContext where Encoders/Decoders
  // are defined only abstractly.
  type Encoder[T] <: GenericEncoder[T, PrepareRow, Session]
  type Decoder[T] <: GenericDecoder[ResultRow, Session, T]
  type NullChecker <: GenericNullChecker[ResultRow, Session]

  // Initial Encoder/Decoder classes that Context implementations will subclass for their
  // respective Encoder[T]/Decoder[T] implementations e.g. JdbcEncoder[T](...) extends BaseEncoder[T]
  type BaseEncoder[T] = GenericEncoder[T, PrepareRow, Session]
  type BaseDecoder[T] = GenericDecoder[ResultRow, Session, T]
  type BaseNullChecker = GenericNullChecker[ResultRow, Session]

  /* ************************************************************************** */

  def lift[T](v: T): T = macro EncodingDslMacro.lift[T]

  @compileTimeOnly(NonQuotedException.message)
  def liftScalar[T](v: T)(implicit e: BaseEncoder[T]): T = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def liftCaseClass[T](v: T): T = NonQuotedException()

  /* ************************************************************************** */

  def liftQuery[U[_] <: Iterable[_], T](v: U[T]): Query[T] = macro EncodingDslMacro.liftQuery[T]

  @compileTimeOnly(NonQuotedException.message)
  def liftQueryScalar[U[_] <: Iterable[_], T](v: U[T])(implicit e: BaseEncoder[T]): Query[T] = NonQuotedException()

  @compileTimeOnly(NonQuotedException.message)
  def liftQueryCaseClass[U[_] <: Iterable[_], T](v: U[T]): Query[T] = NonQuotedException()

  /* ************************************************************************** */

  type MappedEncoding[I, O] = io.getquill.MappedEncoding[I, O]
  val MappedEncoding = io.getquill.MappedEncoding

  implicit def anyValMappedEncoder[I <: AnyVal, O](implicit mapped: MappedEncoding[I, O], encoder: BaseEncoder[O]): Encoder[I] = mappedEncoder

  implicit def anyValMappedDecoder[I, O <: AnyVal](implicit mapped: MappedEncoding[I, O], decoder: BaseDecoder[I]): Decoder[O] = mappedDecoder

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: BaseEncoder[O]): Encoder[I]

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: BaseDecoder[I]): Decoder[O]

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
