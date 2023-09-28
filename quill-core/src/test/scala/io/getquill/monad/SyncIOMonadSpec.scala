package io.getquill.monad

import io.getquill.MirrorContexts

class SyncIOMonadSpec extends IOMonadSpec {

  override val ctx = MirrorContexts.testContext
  import ctx._

  override def eval[T](io: IO[T, _]): T =
    performIO[T](io)

  override def resultValue[T](x: T): Result[T] = x

  "runIO" - {
    "RunQuerySingleResult" in {
      val q = quote(qr1.map(_.i).max)
      eval(ctx.runIO(q)).string mustEqual ctx.run(q).string
    }
    "RunQueryResult" in {
      eval(ctx.runIO(qr1)).string mustEqual ctx.run(qr1).string
    }
    "RunActionResult" in {
      val q = quote(qr1.delete)
      eval(ctx.runIO(q)).string mustEqual ctx.run(q).string
    }
    "RunActionReturningResult" in {
      val t = TestEntity("1", 2, 3L, Some(4), true)
      val q = quote(qr1.insertValue(lift(t)).returning(_.i))
      eval(ctx.runIO(q)).string mustEqual ctx.run(q).string
    }
    "RunBatchActionResult" in {
      val l = List(TestEntity("1", 2, 3L, Some(4), true))
      val q = quote(liftQuery(l).foreach(t => qr1.insertValue(t)))
      eval(ctx.runIO(q)).groups mustEqual ctx.run(q).groups
    }
    "RunBatchActionReturningResult" in {
      val l = List(TestEntity("1", 2, 3L, Some(4), true))
      val q = quote(liftQuery(l).foreach(t => qr1.insertValue(t).returning(_.i)))
      eval(ctx.runIO(q)).groups mustEqual ctx.run(q).groups
    }
  }
}
