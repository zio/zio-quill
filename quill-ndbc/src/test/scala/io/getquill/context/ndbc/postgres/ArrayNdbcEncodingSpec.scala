package io.getquill.context.ndbc.postgres

import io.getquill.context.sql.encoding.ArrayEncodingBaseSpec
import java.time.LocalDate

class ArrayNdbcEncodingSpec extends ArrayEncodingBaseSpec {
  val ctx = testContext
  import ctx._

  val q = quote(query[ArraysTestEntity])

  "Support all sql base types and `Traversable` implementers" in {
    get(ctx.run(q.insert(lift(e))))
    val actual = get(ctx.run(q)).head
    actual mustEqual e
    baseEntityDeepCheck(actual, e)
  }

  "Support Traversable encoding basing on MappedEncoding" in {
    val wrapQ = quote(querySchema[WrapEntity]("ArraysTestEntity"))
    get(ctx.run(wrapQ.insert(lift(wrapE))))
    get(ctx.run(wrapQ)).head mustBe wrapE
  }

  override protected def beforeEach(): Unit = {
    get(ctx.run(q.delete))
    ()
  }
}
