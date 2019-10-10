package io.getquill.monad

import scala.language.higherKinds
import scala.collection.compat._
import language.experimental.macros
import io.getquill.context.Context
import scala.annotation.tailrec
import scala.util.Try

trait SyncIOMonad extends IOMonad {
  this: Context[_, _] =>

  type Result[T] = T

  def runIO[T](quoted: Quoted[T]): IO[RunQuerySingleResult[T], Effect.Read] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[Query[T]]): IO[RunQueryResult[T], Effect.Read] = macro IOMonadMacro.runIO
  def runIO(quoted: Quoted[Action[_]]): IO[RunActionResult, Effect.Write] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[ActionReturning[_, T]]): IO[RunActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIO
  def runIO(quoted: Quoted[BatchAction[Action[_]]]): IO[RunBatchActionResult, Effect.Write] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[BatchAction[ActionReturning[_, T]]]): IO[RunBatchActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIO

  case class Run[T, E <: Effect](f: () => Result[T]) extends IO[T, E]

  def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] = {

    @tailrec def loop[U](io: IO[U, _]): Result[U] = {

      def flatten[Y, M[X] <: IterableOnce[X]](seq: Sequence[Y, M, Effect]) =
        seq.in.iterator.foldLeft(IO.successful(seq.cbfResultToValue.newBuilder)) {
          (builder, item) =>
            builder.flatMap(b => item.map(b += _))
        }.map(_.result())

      io match {
        case FromTry(v)           => v.get
        case Run(f)               => f()
        case seq @ Sequence(_, _) => loop(flatten(seq))
        case TransformWith(a, fA) =>
          a match {
            case FromTry(v)           => loop(fA(v))
            case Run(r)               => loop(fA(Try(r())))
            case seq @ Sequence(_, _) => loop(flatten(seq).transformWith(fA))
            case TransformWith(b, fB) => loop(b.transformWith(fB(_).transformWith(fA)))
            case Transactional(io)    => loop(fA(Try(performIO(io, transactional = true))))
          }
        case Transactional(io) => performIO(io, transactional = true)
      }
    }
    loop(io)
  }
}
