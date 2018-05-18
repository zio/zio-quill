package io.getquill.monad

import io.getquill.context.finagle.postgres.testContext
import com.twitter.util.{ Await, Future }

class TwitterFutureIOMonadSpec extends IOMonadSpec {

  override val ctx = testContext
  import ctx._

  override def eval[T](io: IO[T, _]): T =
    Await.result(performIO(io))

  override def resultValue[T](x: T): Result[T] = Future.value[T](x)

}
