package io.getquill.context.finagle.postgres

import com.twitter.util._
import io.getquill._

class FinaglePostgresContextSpec extends Spec {

  val context = testContext
  import testContext._

  def await[T](f: Future[T]) = Await.result(f)

  "run non-batched action" in {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    await(context.run(insert(lift(1)))) mustEqual 1
  }

  "Insert with returning with single column table" in {
    val inserted: Long = await(testContext.run {
      qr4.insert(lift(TestEntity4(0))).returning(_.i)
    })
    await(testContext.run(qr4.filter(_.i == lift(inserted))))
      .head.i mustBe inserted
  }

  "performIo" in {
    await(performIO(runIO(qr1.filter(_.i == 123)).transactional)) mustBe Nil
  }

  "probe" in {
    val ctx = new FinaglePostgresContext(Literal, "testPostgresDB")
    ctx.probe("select 1").toOption mustBe defined
    ctx.close
  }
}
