package io.getquill.monad

import language.experimental.macros
import scala.util.Failure
import scala.util.Success
import io.getquill.context.Context
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

trait ScalaFutureIOMonad extends IOMonad {
  this: Context[_, _] =>

  type Result[T] = Future[T]

  def runIO[T](quoted: Quoted[T]): IO[RunQuerySingleResult[T], Effect.Read] = macro IOMonadMacro.runIOEC
  def runIO[T](quoted: Quoted[Query[T]]): IO[RunQueryResult[T], Effect.Read] = macro IOMonadMacro.runIOEC
  def runIO(quoted: Quoted[Action[_]]): IO[RunActionResult, Effect.Write] = macro IOMonadMacro.runIOEC
  def runIO[T](quoted: Quoted[ActionReturning[_, T]]): IO[RunActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIOEC
  def runIO(quoted: Quoted[BatchAction[Action[_]]]): IO[RunBatchActionResult, Effect.Write] = macro IOMonadMacro.runIOEC
  def runIO[T](quoted: Quoted[BatchAction[ActionReturning[_, T]]]): IO[RunBatchActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIOEC

  case class Run[T, E <: Effect](f: (ExecutionContext) => Result[T]) extends IO[T, E]

  def performIO[T](io: IO[T, _], transactional: Boolean = false)(implicit ec: ExecutionContext): Result[T] =
    io match {
      case FromTry(v) => Future.fromTry(v)
      case Run(f)     => f(ec)
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
