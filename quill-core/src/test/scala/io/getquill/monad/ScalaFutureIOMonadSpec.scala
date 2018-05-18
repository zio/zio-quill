package io.getquill.monad

import scala.concurrent.Future

class ScalaFutureIOMonadSpec extends IOMonadSpec {

  override val ctx = io.getquill.testAsyncContext
  import ctx._

  override def eval[T](io: IO[T, _]) = {
    ctx.eval(ctx.performIO(io))
  }

  override def resultValue[T](x: T): Result[T] = Future.successful(x)
}
