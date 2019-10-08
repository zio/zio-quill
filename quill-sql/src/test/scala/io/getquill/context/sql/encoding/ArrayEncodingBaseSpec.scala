package io.getquill.context.sql.encoding

import java.time.LocalDate
import java.util.Date

import io.getquill.{ MappedEncoding, Spec }
import org.scalatest.{ Assertion, BeforeAndAfterEach }

trait ArrayEncodingBaseSpec extends Spec with BeforeAndAfterEach {

  // Support all sql base types and `Seq` implementers
  case class ArraysTestEntity(
    texts:      List[String],
    decimals:   Seq[BigDecimal],
    bools:      Vector[Boolean],
    bytes:      List[Byte],
    shorts:     IndexedSeq[Short],
    ints:       Seq[Int],
    longs:      Seq[Long],
    floats:     Seq[Float],
    doubles:    Seq[Double],
    timestamps: Seq[Date],
    dates:      Seq[LocalDate]
  )

  val e = ArraysTestEntity(List("test"), Seq(BigDecimal(2.33)), Vector(true, true), List(1),
    IndexedSeq(3), Seq(2), Seq(1, 2, 3), Seq(1f, 2f), Seq(4d, 3d),
    Seq(new Date(System.currentTimeMillis())), Seq(LocalDate.now()))

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

  // Support Seq encoding basing on MappedEncoding
  case class StrWrap(str: String)
  implicit val strWrapEncode: MappedEncoding[StrWrap, String] = MappedEncoding(_.str)
  implicit val strWrapDecode: MappedEncoding[String, StrWrap] = MappedEncoding(StrWrap.apply)
  case class WrapEntity(texts: Seq[StrWrap])
  val wrapE = WrapEntity(List("hey", "ho").map(StrWrap.apply))
}
