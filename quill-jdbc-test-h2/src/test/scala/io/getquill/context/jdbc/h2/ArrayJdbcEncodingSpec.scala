package io.getquill.context.jdbc.h2

import io.getquill.context.sql.encoding.ArrayEncodingBaseSpec

import java.sql.Timestamp

class ArrayJdbcEncodingSpec extends ArrayEncodingBaseSpec {
  val ctx = testContext
  import ctx._

  val q         = quote(query[ArraysTestEntity])
  val corrected = e.copy(timestamps = e.timestamps.map(d => new Timestamp(d.getTime)))

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
    ()
  }

  "Support all sql base types and `Seq` implementers" in {
    ctx.run(q.insertValue(lift(corrected)))
    val actual = ctx.run(q).head
    actual mustEqual corrected
    baseEntityDeepCheck(actual, corrected)
  }

  "Support Seq encoding basing on MappedEncoding" in {
    val wrapQ = quote(querySchema[WrapEntity]("ArraysTestEntity"))
    ctx.run(wrapQ.insertValue(lift(wrapE)))
    ctx.run(wrapQ).head.texts mustBe wrapE.texts
  }

  "empty array on found null" in {
    case class ArraysTestEntity(texts: Option[List[String]])
    ctx.run(query[ArraysTestEntity].insertValue(lift(ArraysTestEntity(None))))

    case class E(texts: List[String])
    ctx.run(querySchema[E]("ArraysTestEntity")).headOption.map(_.texts) mustBe Some(Nil)
  }

}
