package io.getquill.monad

import io.getquill.context.finagle.mysql.testContext
import com.twitter.util.Await

class TwitterFutureIOMonadSpec extends IOMonadSpec {

  val ctx = testContext
  import ctx._

  def eval[T](io: IO[T, _]): T =
    Await.result(performIO(io))
}