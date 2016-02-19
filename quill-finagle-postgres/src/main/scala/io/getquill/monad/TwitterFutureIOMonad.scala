package io.getquill.monad

import com.twitter.util.Future
import io.getquill.context.Context
import com.twitter.util.Return
import scala.util.Success
import com.twitter.util.Throw
import scala.util.Failure
import com.twitter.util.Try

trait TwitterFutureIOMonad extends IOMonad {
  this: Context[_, _] =>

  type Result[T] = Future[T]

  def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] =
    io match {
      case FromTry(t) => Future.const(Try(t.get))
      case Run(f)     => f()
      case Sequence(in, _, cbf) =>
        Future.collect(in.map(performIO(_)).toSeq)
          .map(r => cbf().++=(r).result)
      case TransformWith(a, fA) =>
        performIO(a)
          .liftToTry.map {
            case Return(v) => Success(v)
            case Throw(t)  => Failure(t)
          }
          .flatMap(v => performIO(fA(v)))
      case Transactional(io) =>
        performIO(io, transactional = true)
    }
}
