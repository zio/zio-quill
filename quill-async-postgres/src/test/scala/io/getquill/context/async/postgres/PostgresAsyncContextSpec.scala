package io.getquill.context.async.postgres

import com.github.mauricio.async.db.QueryResult

import scala.concurrent.ExecutionContext.Implicits.global
import io.getquill.{ Literal, PostgresAsyncContext, Spec }

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
      qr4.insert(lift(TestEntity4(0))).returning(_.i)
    })
    await(testContext.run(qr4.filter(_.i == lift(inserted))))
      .head.i mustBe inserted
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
        returningColumn:    String,
        returningExtractor: ctx.Extractor[O]
      )(result: QueryResult) =
        super.extractActionResult(returningColumn, returningExtractor)(result)
    }
    intercept[IllegalStateException] {
      ctx.extractActionResult("w/e", row => 1)(new QueryResult(0, "w/e"))
    }
    ctx.close
  }

  override protected def beforeAll(): Unit = {
    await(testContext.run(qr1.delete))
    ()
  }
}
