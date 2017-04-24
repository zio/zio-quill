package io.getquill.context.jdbc.postgres

import java.time.LocalDate

import io.getquill.{ Literal, PostgresJdbcContext }
import io.getquill.context.sql.dsl.ArrayEncodingSpec

class ArrayJdbcEncodingSpec extends ArrayEncodingSpec {
  val ctx = testContext
  import ctx._

  "Support all sql base types and `Traversable` implementers" in {
    ctx.run(q.insert(lift(e)))
    val actual = ctx.run(q).head
    actual mustEqual e
    baseEntityDeepCheck(actual, e)
  }

  implicit val strWrapEncode: MappedEncoding[StrWrap, String] = MappedEncoding(_.str)
  implicit val strWrapDecode: MappedEncoding[String, StrWrap] = MappedEncoding(StrWrap.apply)
  "Support Traversable encoding basing on MappedEncoding" in {
    ctx.run(wrapQ.insert(lift(wrapE)))
    ctx.run(wrapQ).head mustBe wrapE
  }

  "Catch invalid decoders" in {
    val newCtx = new PostgresJdbcContext[Literal]("testPostgresDB") {
      // avoid transforming from java.sql.Date to java.time.LocalDate
      override implicit def arrayLocalDateDecoder[Col <: Traversable[LocalDate]](implicit bf: CBF[LocalDate, Col]): Decoder[Col] =
        arrayDecoder[LocalDate, LocalDate, Col](identity)
    }
    import newCtx._
    newCtx.run(query[ArraysTestEntity].insert(lift(e)))
    intercept[IllegalArgumentException] {
      newCtx.run(query[ArraysTestEntity]).head mustBe e
    }
    newCtx.close()
  }

  override protected def beforeEach(): Unit = {
    ctx.run(q.delete)
    ()
  }
}
