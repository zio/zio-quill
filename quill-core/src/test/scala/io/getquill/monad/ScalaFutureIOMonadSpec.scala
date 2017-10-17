package io.getquill.monad

class ScalaFutureIOMonadSpec extends IOMonadSpec {

  override val ctx = io.getquill.testAsyncContext
  import ctx._

  override def eval[T](io: IO[T, _]) = {
    ctx.eval(ctx.performIO(io))
  }
}
