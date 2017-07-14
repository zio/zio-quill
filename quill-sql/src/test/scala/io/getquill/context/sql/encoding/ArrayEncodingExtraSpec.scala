package io.getquill.context.sql.encoding

import java.time.{ ZonedDateTime }

import io.getquill.{ MappedEncoding, Spec }
import org.scalatest.{ Assertion, BeforeAndAfterEach }

trait ArrayEncodingExtraSpec extends Spec with BeforeAndAfterEach {

  // Support all sql base types and `Seq` implementers
  case class ArraysExtraTestEntity(
    dates: Seq[ZonedDateTime]
  )

  val e = ArraysExtraTestEntity(Seq(ZonedDateTime.now()))

  // casting types can be dangerous so we need to ensure that everything is ok
  def baseEntityDeepCheck(e1: ArraysExtraTestEntity, e2: ArraysExtraTestEntity): Assertion = {
    e1.dates.head mustBe e2.dates.head
  }

  // Support Seq encoding basing on MappedEncoding
  case class StrWrap(str: String)
  implicit val strWrapEncode: MappedEncoding[StrWrap, String] = MappedEncoding(_.str)
  implicit val strWrapDecode: MappedEncoding[String, StrWrap] = MappedEncoding(StrWrap.apply)
  case class WrapEntity(texts: Seq[StrWrap])
  val wrapE = WrapEntity(List("hey", "ho").map(StrWrap.apply))
}
