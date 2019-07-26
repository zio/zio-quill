package io.getquill.context.finagle.postgres

import com.twitter.util._
import io.getquill._
import org.scalatest.BeforeAndAfter

class FinaglePostgresContextSpec extends Spec with BeforeAndAfter {

  val context = testContext
  import testContext._

  def await[T](f: Future[T]) = Await.result(f)

  before {
    await(testContext.run(qr1.delete))
  }

  val insert = quote { (i: Int) =>
    qr1.insert(_.i -> i)
  }

  "run non-batched action" in {
    await(context.run(insert(lift(1)))) mustEqual 1
  }

  "Insert with returning with single column table" in {
    val inserted: Long = await(testContext.run {
      qr4.insert(lift(TestEntity4(0))).returningGenerated(_.i)
    })
    await(testContext.run(qr4.filter(_.i == lift(inserted))))
      .head.i mustBe inserted
  }

  "performIo" in {
    await(context.run(insert(lift(1))))
    await(performIO(runIO(qr1.filter(_.i == 123)).transactional)) mustBe Nil
  }

  "probe" in {
    val ctx = new FinaglePostgresContext(Literal, "testPostgresDB")
    ctx.probe("select 1").toOption mustBe defined
    ctx.close
  }

  "prepare" in {
    import com.twitter.finagle.postgres.Param
    import com.twitter.finagle.postgres.values.ValueEncoder._

    testContext.prepareParams(
      "", ps => (Nil, ps ++: List(Param("Sarah"), Param(127)))
    ) mustEqual List("'Sarah'", "'127'")
  }
}
