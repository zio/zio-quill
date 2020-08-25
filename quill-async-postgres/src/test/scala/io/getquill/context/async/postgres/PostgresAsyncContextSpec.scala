package io.getquill.context.async.postgres

import com.github.mauricio.async.db.QueryResult
import io.getquill.ReturnAction.ReturnColumns

import scala.concurrent.ExecutionContext.Implicits.global
import io.getquill.{ Literal, PostgresAsyncContext, ReturnAction, Spec }

class PostgresAsyncContextSpec extends Spec {

  import testContext._

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(testContext.run(insert(lift(1)))) mustEqual 1
  }

  "Insert with returning with single column table" in {
    val inserted: Long = await(testContext.run {
      qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
    })
    await(testContext.run(qr4.filter(_.i == lift(inserted))))
      .head.i mustBe inserted
  }
  "Insert with returning with multiple columns" in {
    await(testContext.run(qr1.delete))
    val inserted = await(testContext.run {
      qr1.insert(lift(TestEntity("foo", 1, 18L, Some(123), true))).returning(r => (r.i, r.s, r.o))
    })
    (1, "foo", Some(123)) mustBe inserted
  }

  "performIO" in {
    await(performIO(runIO(qr4).transactional))
  }

  "probe" in {
    probe("select 1").toOption mustBe defined
  }

  "cannot extract" in {
    object ctx extends PostgresAsyncContext(Literal, "testPostgresDB") {
      override def extractActionResult[O](
        returningAction:    ReturnAction,
        returningExtractor: ctx.Extractor[O]
      )(result: QueryResult) =
        super.extractActionResult(returningAction, returningExtractor)(result)
    }
    intercept[IllegalStateException] {
      ctx.extractActionResult(ReturnColumns(List("w/e")), row => 1)(new QueryResult(0, "w/e"))
    }
    ctx.close
  }

  "prepare" in {
    testContext.prepareParams("", ps => (Nil, ps ++ List("Sarah", 127))) mustEqual List("'Sarah'", "127")
  }

  override protected def beforeAll(): Unit = {
    await(testContext.run(qr1.delete))
    ()
  }
}
