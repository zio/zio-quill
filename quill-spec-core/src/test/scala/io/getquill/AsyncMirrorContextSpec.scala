package io.getquill

class AsyncMirrorContextSpec extends Spec {
  val ctx = testAsyncContext
  import ctx._

  "executeQuery" in {
    eval(ctx.run(qr1))
  }

  "executeQuerySingle" in {
    eval(ctx.run(qr1.map(_.i).max))
  }

  "executeAction" in {
    eval(ctx.run(qr4.insert(lift(TestEntity4(1)))))
  }

  "executeActionReturning" in {
    eval(ctx.run(qr4.insert(lift(TestEntity4(0))).returning(_.i)))
  }

  "executeBatchAction" in {
    eval(ctx.run {
      liftQuery(List(TestEntity4(1))).foreach(e => qr4.insert(e))
    })
  }

  "executeBatchActionReturning" in {
    eval(ctx.run {
      liftQuery(List(TestEntity4(0))).foreach(e => qr4.insert(e).returning(_.i))
    })
  }

  "prepare" in {
    ctx.prepareParams("", ps => (Nil, ps.add("Sarah").add(127))) mustEqual List("'Sarah'", "127")
  }

  "probe" in {
    ctx.probe("Ok").toOption mustBe defined
    ctx.probe("Fail").toOption mustBe empty
  }

  "close" in {
    ctx.close()
  }
}
