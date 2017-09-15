package io.getquill.monad

import scala.util.Failure
import scala.util.Success
import io.getquill.context.Context
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait ScalaFutureIOMonad extends IOMonad {
  this: Context[_, _] =>

  type Result[T] = Future[T]

  def performIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] =
    io match {
      case FromTry(v) => Future.fromTry(v)
      case Run(f)     => f()
      case Sequence(in, cbfIOToResult, cbfResultToValue) =>
        val builder = cbfIOToResult()
        in.foreach(builder += performIO(_))
        Future.sequence(builder.result)(cbfResultToValue, ec)
      case TransformWith(a, fA) =>
        performIO(a)
          .map(Success(_))
          .recover { case ex => Failure(ex) }
          .flatMap(v => performIO(fA(v)))
      case Transactional(io) =>
        performIO(io, transactional = true)
    }
}
