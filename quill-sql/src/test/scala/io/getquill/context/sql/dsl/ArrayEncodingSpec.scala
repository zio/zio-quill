package io.getquill.context.sql.dsl

import java.sql.Timestamp
import java.time.LocalDate
import java.util.Date

import io.getquill.Spec
import io.getquill.context.sql.SqlContext
import org.scalatest.{ Assertion, BeforeAndAfterEach }

import scala.collection.mutable.ListBuffer

trait ArrayEncodingSpec extends Spec with BeforeAndAfterEach {
  val ctx: SqlContext[_, _] with ArrayEncoding
  import ctx._

  // Support all sql base types and `Traversable` implementers
  case class ArraysTestEntity(
    texts:      List[String],
    decimals:   Seq[BigDecimal],
    bools:      Vector[Boolean],
    bytes:      ListBuffer[Byte],
    shorts:     Iterable[Short],
    ints:       IndexedSeq[Int],
    longs:      Set[Long],
    floats:     Traversable[Float],
    doubles:    Seq[Double],
    timestamps: Seq[Date],
    dates:      Seq[LocalDate]
  )
  val q = quote(query[ArraysTestEntity])
  val e = ArraysTestEntity(List("test"), Seq(BigDecimal(2.33)), Vector(true, true), ListBuffer(1),
    Iterable(3), IndexedSeq(2), Set(1, 2, 3), Traversable(1f, 2f), Seq(4d, 3d),
    Seq(new Timestamp(System.currentTimeMillis())), Seq(LocalDate.now()))

  // casting types can be dangerous so we need to ensure that everything is ok
  def baseEntityDeepCheck(e1: ArraysTestEntity, e2: ArraysTestEntity): Assertion = {
    e1.texts.head mustBe e2.texts.head
    e1.decimals.head mustBe e2.decimals.head
    e1.bools.head mustBe e2.bools.head
    e1.bytes.head mustBe e2.bytes.head
    e1.shorts.head mustBe e2.shorts.head
    e1.ints.head mustBe e2.ints.head
    e1.longs.head mustBe e2.longs.head
    e1.floats.head mustBe e2.floats.head
    e1.doubles.head mustBe e2.doubles.head
    e1.timestamps.head mustBe e2.timestamps.head
    e1.dates.head mustBe e2.dates.head
  }

  // Support Traversable encoding basing on MappedEncoding
  case class StrWrap(str: String)
  // it fails with NPE on the line above, moving this into Spec implementation fixed that, why?
  //implicit val strWrapEncode: MappedEncoding[StrWrap, String] = MappedEncoding(_.toString)
  //implicit val strWrapDecode: MappedEncoding[String, StrWrap] = MappedEncoding(StrWrap.apply)
  case class WrapEntity(texts: Seq[StrWrap])
  val wrapQ = quote(querySchema[WrapEntity]("ArraysTestEntity"))
  val wrapE = WrapEntity(List("hey", "ho").map(StrWrap.apply))
}
