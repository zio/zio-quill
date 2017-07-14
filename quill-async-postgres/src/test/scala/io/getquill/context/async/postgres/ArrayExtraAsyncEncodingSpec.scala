package io.getquill.context.async.postgres

import java.time.{ ZonedDateTime }

import io.getquill.context.sql.encoding.ArrayEncodingExtraSpec
import org.joda.time.{ DateTime => JodaDateTime }

import scala.concurrent.ExecutionContext.Implicits.global

class ArrayExtraAsyncEncodingSpec extends ArrayEncodingExtraSpec {
  val ctx = testContext
  import ctx._

  val q = quote(query[ArraysExtraTestEntity])

  "Support all sql base types and `Traversable` implementers" in {
    await(ctx.run(q.insert(lift(e))))
    val actual = await(ctx.run(q)).head
    actual mustEqual e
    baseEntityDeepCheck(actual, e)
  }

  "Joda times" in {
    case class JodaTimes(dates: Seq[JodaDateTime])
    val jE = JodaTimes(Seq(JodaDateTime.now()))
    val jQ = quote(querySchema[JodaTimes]("ArraysExtraTestEntity"))
    await(ctx.run(jQ.insert(lift(jE))))
    val actual = await(ctx.run(jQ)).head
    actual.dates mustBe jE.dates
  }

  "Support Traversable encoding basing on MappedEncoding" in {
    val wrapQ = quote(querySchema[WrapEntity]("ArraysExtraTestEntity"))
    await(ctx.run(wrapQ.insert(lift(wrapE))))
    await(ctx.run(wrapQ)).head mustBe wrapE
  }

  "Catch invalid decoders" in {
    val newCtx = new TestContext {
      // avoid transforming from org.joda.time.DateTime to java.time.ZonedDateTime
      override implicit def arrayZonedDateTimeDecoder[Col <: Seq[ZonedDateTime]](implicit bf: CBF[ZonedDateTime, Col]): Decoder[Col] =
        arrayDecoder[ZonedDateTime, ZonedDateTime, Col](identity)
    }
    import newCtx._
    await(newCtx.run(query[ArraysExtraTestEntity].insert(lift(e))))
    intercept[IllegalStateException] {
      await(newCtx.run(query[ArraysExtraTestEntity])).head mustBe e
    }
    newCtx.close()
  }

  override protected def beforeEach(): Unit = {
    await(ctx.run(q.delete))
    ()
  }
}
