package io.getquill.doobie

import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.SqlContext
import io.getquill.NamingStrategy
import io.getquill.context.Context
import doobie.util.composite.Composite
import doobie.util.meta.Meta
import java.util.Date
import java.time.LocalDate
import java.util.UUID
import doobie._
import java.sql.ResultSet

class DoobieContext[Dialect <: SqlIdiom, Naming <: NamingStrategy](val idiom: Dialect, val naming: Naming)
  extends Context[Dialect, Naming]
  with SqlContext[Dialect, Naming]
  with Encoders
  with Decoders {

  override type PrepareRow = FPS.PreparedStatementIO[Unit]
  override type ResultRow = ResultSet

  override type Result[T] = ConnectionIO[T]
  override type RunQueryResult[T] = List[T]
  override type RunQuerySingleResult[T] = T
  override type RunActionResult = Long
  override type RunActionReturningResult[T] = T
  override type RunBatchActionResult = List[Long]
  override type RunBatchActionReturningResult[T] = List[T]

  implicit val uuidMeta = implicitly[Meta[String]].xmap(UUID.fromString, (_: UUID).toString)

  def close(): Unit = ???
  def probe(statement: String): scala.util.Try[_] = ???

  def executeQuery[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): List[T] =
    ???

  def executeQuerySingle[T](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[T] = identityExtractor): T =
    ???

  def executeAction[T](sql: String, prepare: Prepare = identityPrepare): Long =
    ???

  def executeActionReturning[O](sql: String, prepare: Prepare = identityPrepare, extractor: Extractor[O], returningColumn: String): O =
    ???

  def executeBatchAction(groups: List[BatchGroup]): List[Long] =
    ???

  def executeBatchActionReturning[T](groups: List[BatchGroupReturning], extractor: Extractor[T]): List[T] =
    ???
}

trait Encoders {
  this: DoobieContext[_, _] =>

  case class DoobieEncoder[T]()(implicit c: Meta[T]) extends BaseEncoder[T] {
    override def apply(idx: Int, value: T, ps: FPS.PreparedStatementIO[Unit]) =
      ps.flatMap(_ => Composite.fromMeta[T].set(idx, value))
  }
  type Encoder[T] = DoobieEncoder[T]

  implicit def encoder[T: Meta] = DoobieEncoder[T]

  implicit val stringEncoder: Encoder[String] = encoder[String]
  implicit val bigDecimalEncoder: Encoder[BigDecimal] = encoder[BigDecimal]
  implicit val booleanEncoder: Encoder[Boolean] = encoder[Boolean]
  implicit val byteEncoder: Encoder[Byte] = encoder[Byte]
  implicit val shortEncoder: Encoder[Short] = encoder[Short]
  implicit val intEncoder: Encoder[Int] = encoder[Int]
  implicit val longEncoder: Encoder[Long] = encoder[Long]
  implicit val floatEncoder: Encoder[Float] = encoder[Float]
  implicit val doubleEncoder: Encoder[Double] = encoder[Double]
  implicit val byteArrayEncoder: Encoder[Array[Byte]] = encoder[Array[Byte]]
  implicit val dateEncoder: Encoder[Date] = encoder[Date]
  implicit val localDateEncoder: Encoder[LocalDate] = encoder[LocalDate]
  implicit val uuidEncoder: Encoder[UUID] = encoder[UUID]

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], e: Encoder[O]): Encoder[I] =
    //    mappedBaseEncoder(mapped, e.encoder)
    ???

  implicit def optionEncoder[T](implicit d: Encoder[T]): Encoder[Option[T]] = ???
}

trait Decoders {
  this: DoobieContext[_, _] =>

  case class DoobieDecoder[T]()(implicit c: Meta[T]) extends BaseDecoder[T] {
    override def apply(idx: Int, rs: ResultSet) =
      c.unsafeGetNonNullable(rs, idx)
  }
  type Decoder[T] = DoobieDecoder[T]

  implicit def decoder[T: Meta] = DoobieDecoder[T]

  implicit val stringDecoder: Decoder[String] = decoder[String]
  implicit val bigDecimalDecoder: Decoder[BigDecimal] = decoder[BigDecimal]
  implicit val booleanDecoder: Decoder[Boolean] = decoder[Boolean]
  implicit val byteDecoder: Decoder[Byte] = decoder[Byte]
  implicit val shortDecoder: Decoder[Short] = decoder[Short]
  implicit val intDecoder: Decoder[Int] = decoder[Int]
  implicit val longDecoder: Decoder[Long] = decoder[Long]
  implicit val floatDecoder: Decoder[Float] = decoder[Float]
  implicit val doubleDecoder: Decoder[Double] = decoder[Double]
  implicit val byteArrayDecoder: Decoder[Array[Byte]] = decoder[Array[Byte]]
  implicit val dateDecoder: Decoder[Date] = decoder[Date]
  implicit val localDateDecoder: Decoder[LocalDate] = decoder[LocalDate]
  implicit val uuidDecoder: Decoder[UUID] = decoder[UUID]

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], d: Decoder[I]): Decoder[O] =
    ???

  implicit def optionDecoder[T](implicit d: Decoder[T]): Decoder[Option[T]] = ???
}