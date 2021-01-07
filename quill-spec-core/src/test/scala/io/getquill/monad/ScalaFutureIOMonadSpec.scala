package io.getquill.monad

import scala.concurrent.Future
import scala.concurrent.ExecutionContext

class ScalaFutureIOMonadSpec extends IOMonadSpec {

  override val ctx = io.getquill.testAsyncContext
  import ctx._

  override def eval[T](io: IO[T, _]) = {
    ctx.eval(ctx.performIO(io))
  }

  override def resultValue[T](x: T): Result[T] = Future.successful(x)

  "runIO" - {
    "RunQuerySingleResult" in {
      val q = quote(qr1.map(_.i).max)
      eval(ctx.runIO(q)).string mustEqual ctx.eval(ctx.run(q)).string
    }
    "RunQueryResult" in {
      eval(ctx.runIO(qr1)).string mustEqual ctx.eval(ctx.run(qr1)).string
    }
    "RunActionResult" in {
      val q = quote(qr1.delete)
      eval(ctx.runIO(q)).string mustEqual ctx.eval(ctx.run(q)).string
    }
    "RunActionReturningResult" in {
      val t = TestEntity("1", 2, 3L, Some(4), true)
      val q = quote(qr1.insert(lift(t)).returning(_.i))
      eval(ctx.runIO(q)).string mustEqual ctx.eval(ctx.run(q)).string
    }
    "RunBatchActionResult" in {
      val l = List(TestEntity("1", 2, 3L, Some(4), true))
      val q = quote(liftQuery(l).foreach(t => qr1.insert(t)))
      eval(ctx.runIO(q)).groups mustEqual ctx.eval(ctx.run(q)).groups
    }
    "RunBatchActionReturningResult" in {
      val l = List(TestEntity("1", 2, 3L, Some(4), true))
      val q = quote(liftQuery(l).foreach(t => qr1.insert(t).returning(_.i)))
      eval(ctx.runIO(q)).groups mustEqual ctx.eval(ctx.run(q)).groups
    }
    "transactional" in {
      val l = List(TestEntity("1", 2, 3L, Some(4), true))
      val q = quote(liftQuery(l).foreach(t => qr1.insert(t).returning(_.i)))
      eval(ctx.runIO(q).transactional).ec mustEqual TransactionalExecutionContext(implicitly[ExecutionContext])
    }
  }
}
