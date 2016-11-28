package io.getquill.monad

import scala.util.Failure
import scala.util.Success
import io.getquill.context.Context
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait ScalaFutureIOMonad extends IOMonad {
  this: Context[_, _] =>

  type Result[T] = Future[T]

  def unsafePerformIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] =
    io match {
      case FromTry(v) => Future.fromTry(v)
      case Run(f)     => f()
      case Sequence(in, cbfIOToResult, cbfResultToValue) =>
        val builder = cbfIOToResult()
        in.foreach(builder += unsafePerformIO(_))
        Future.sequence(builder.result)(cbfResultToValue, ec)
      case TransformWith(a, fA) =>
        unsafePerformIO(a)
          .map(Success(_))
          .recover { case ex => Failure(ex) }
          .flatMap(v => unsafePerformIO(fA(v)))
      case Transactional(io) =>
        unsafePerformIO(io, transactional = true)
    }
}
