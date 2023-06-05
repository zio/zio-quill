package io.getquill.context.zio

import io.getquill.context.Context
import io.getquill.monad.{IOMonad, IOMonadMacro}
import io.getquill.{Action, ActionReturning, BatchAction, Query, Quoted}
import zio.{RIO, ZIO}

import scala.collection.compat._
import scala.language.experimental.macros
import scala.language.higherKinds
import scala.util.{Failure, Success}

trait ZIOMonad extends IOMonad {
  this: Context[_, _] =>

  type Result[T] = RIO[ZioJAsyncConnection, T]

  def runIO[T](quoted: Quoted[T]): IO[RunQuerySingleResult[T], Effect.Read] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[Query[T]]): IO[RunQueryResult[T], Effect.Read] = macro IOMonadMacro.runIO
  def runIO(quoted: Quoted[Action[_]]): IO[RunActionResult, Effect.Write] = macro IOMonadMacro.runIO
  def runIO[T](
    quoted: Quoted[ActionReturning[_, T]]
  ): IO[RunActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIO
  def runIO(
    quoted: Quoted[BatchAction[Action[_]]]
  ): IO[RunBatchActionResult, Effect.Write] = macro IOMonadMacro.runIO
  def runIO[T](
    quoted: Quoted[BatchAction[ActionReturning[_, T]]]
  ): IO[RunBatchActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIO

  case class Run[T, E <: Effect](f: () => Result[T]) extends IO[T, E]

  def flatten[Y, M[X] <: IterableOnce[X]](
    seq: Sequence[Y, M, Effect]
  ) = {
    val builder = seq.cbfResultToValue.newBuilder
    ZIO
      .foldLeft(seq.in.iterator.toIterable)(builder) { (r, ioa) =>
        for {
          a <- performIO(ioa)
        } yield r += a
      }
      .map(_.result())
  }

  def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] =
    io match {
      case FromTry(v) => ZIO.fromTry(v)
      case Run(f)     => f()
      case seq @ Sequence(_, _) =>
        flatten(seq)
      case TransformWith(a, fA) =>
        performIO(a).either.flatMap(valueOrError => performIO(fA(valueOrError.fold(Failure(_), Success(_)))))
      case Transactional(io) =>
        performIO(io, transactional = true)
    }
}
