package io.getquill.monad

import language.experimental.macros
import com.twitter.util.Future
import io.getquill.context.Context
import com.twitter.util.Try
import scala.collection.compat._
import io.getquill.{ Query, Action, BatchAction, ActionReturning }

trait TwitterFutureIOMonad extends IOMonad {
  this: Context[_, _] =>

  type Result[T] = Future[T]

  def runIO[T](quoted: Quoted[T]): IO[RunQuerySingleResult[T], Effect.Read] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[Query[T]]): IO[RunQueryResult[T], Effect.Read] = macro IOMonadMacro.runIO
  def runIO(quoted: Quoted[Action[_]]): IO[RunActionResult, Effect.Write] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[ActionReturning[_, T]]): IO[RunActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIO
  def runIO(quoted: Quoted[BatchAction[Action[_]]]): IO[RunBatchActionResult, Effect.Write] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[BatchAction[ActionReturning[_, T]]]): IO[RunBatchActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIO

  case class Run[T, E <: Effect](f: () => Result[T]) extends IO[T, E]

  def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] =
    io match {
      case FromTry(t) => Future.const(Try.fromScala(t))
      case Run(f)     => f()
      case Sequence(in, cbf) =>
        Future.collect(in.iterator.map(performIO(_)).iterator.to(Seq))
          .map(r => cbf.newBuilder.++=(r).result)
      case TransformWith(a, fA) =>
        performIO(a)
          .liftToTry.map(_.asScala)
          .flatMap(v => performIO(fA(v)))
      case Transactional(io) =>
        performIO(io, transactional = true)
    }
}
