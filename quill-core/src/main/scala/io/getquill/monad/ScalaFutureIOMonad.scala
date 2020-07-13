package io.getquill.monad

import io.getquill.context.Context
import language.experimental.macros
import language.higherKinds
import scala.collection.compat._
import scala.util.Failure
import scala.util.Success

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import io.getquill.{ Query, Action, BatchAction, ActionReturning }

trait ScalaFutureIOMonad extends IOMonad {
  this: Context[_, _] =>

  type Result[T] = Future[T]

  def runIO[T](quoted: Quoted[T]): IO[RunQuerySingleResult[T], Effect.Read] = macro IOMonadMacro.runIOEC
  def runIO[T](quoted: Quoted[Query[T]]): IO[RunQueryResult[T], Effect.Read] = macro IOMonadMacro.runIOEC
  def runIO(quoted: Quoted[Action[_]]): IO[RunActionResult, Effect.Write] = macro IOMonadMacro.runIOEC
  def runIO[T](
    quoted: Quoted[ActionReturning[_, T]]
  ): IO[RunActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIOEC
  def runIO(
    quoted: Quoted[BatchAction[Action[_]]]
  ): IO[RunBatchActionResult, Effect.Write] = macro IOMonadMacro.runIOEC
  def runIO[T](
    quoted: Quoted[BatchAction[ActionReturning[_, T]]]
  ): IO[RunBatchActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIOEC

  case class Run[T, E <: Effect](f: (ExecutionContext) => Result[T])
    extends IO[T, E]

  def flatten[Y, M[X] <: IterableOnce[X]](
    seq: Sequence[Y, M, Effect]
  )(implicit ec: ExecutionContext) = {
    val builder = seq.cbfResultToValue.newBuilder
    seq.in.iterator
      .foldLeft(Future.successful(builder)) { (fr, ioa) =>
        for {
          r <- fr
          a <- performIO(ioa)
        } yield r += a
      }
      .map(_.result())
  }

  def performIO[T](io: IO[T, _], transactional: Boolean = false)(
    implicit
    ec: ExecutionContext
  ): Result[T] =
    io match {
      case FromTry(v) => Future.fromTry(v)
      case Run(f)     => f(ec)
      case seq @ Sequence(_, _) =>
        flatten(seq)
      case TransformWith(a, fA) =>
        performIO(a)
          .map(Success(_))
          .recover { case ex => Failure(ex) }
          .flatMap(v => performIO(fA(v)))
      case Transactional(io) =>
        performIO(io, transactional = true)
    }
}
