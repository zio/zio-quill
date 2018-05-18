package io.getquill.monad

import scala.language.experimental.macros
import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds
import scala.util.Failure
import scala.util.Success
import scala.util.Try
import io.getquill.context.Context
import io.getquill.util.Messages.fail

sealed trait Effect

object Effect {
  trait Read extends Effect
  trait Write extends Effect
  trait Transaction extends Effect
}

trait IOMonad {
  this: Context[_, _] =>

  type Effect = io.getquill.monad.Effect
  val Effect = io.getquill.monad.Effect

  def runIO[T](quoted: Quoted[T]): IO[RunQuerySingleResult[T], Effect.Read] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[Query[T]]): IO[RunQueryResult[T], Effect.Read] = macro IOMonadMacro.runIO
  def runIO(quoted: Quoted[Action[_]]): IO[RunActionResult, Effect.Write] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[ActionReturning[_, T]]): IO[RunActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIO
  def runIO(quoted: Quoted[BatchAction[Action[_]]]): IO[RunBatchActionResult, Effect.Write] = macro IOMonadMacro.runIO
  def runIO[T](quoted: Quoted[BatchAction[ActionReturning[_, T]]]): IO[RunBatchActionReturningResult[T], Effect.Write] = macro IOMonadMacro.runIO

  case class Run[T, E <: Effect](f: () => Result[T]) extends IO[T, E]
  protected case class FromTry[T](t: Try[T]) extends IO[T, Effect]
  protected case class Sequence[A, M[X] <: TraversableOnce[X], E <: Effect](in: M[IO[A, E]], cbfIOToResult: CanBuildFrom[M[IO[A, E]], Result[A], M[Result[A]]], cbfResultToValue: CanBuildFrom[M[Result[A]], A, M[A]]) extends IO[M[A], E]
  protected case class TransformWith[T, S, E1 <: Effect, E2 <: Effect](io: IO[T, E1], f: Try[T] => IO[S, E2]) extends IO[S, E1 with E2]
  protected case class Transactional[T, E <: Effect](io: IO[T, E]) extends IO[T, E with Effect.Transaction]

  object IO {

    def fromTry[T](result: Try[T]): IO[T, Effect] = FromTry(result)

    def sequence[A, M[X] <: TraversableOnce[X], E <: Effect](in: M[IO[A, E]])(implicit cbfIOToResult: CanBuildFrom[M[IO[A, E]], Result[A], M[Result[A]]], cbfResultToValue: CanBuildFrom[M[Result[A]], A, M[A]]): IO[M[A], E] =
      Sequence(in, cbfIOToResult, cbfResultToValue)

    val unit: IO[Unit, Effect] = fromTry(Success(()))

    def zip[T, E1 <: Effect, S, E2 <: Effect](a: IO[T, E1], b: IO[S, E2]): IO[(T, S), E1 with E2] =
      sequence(List(a, b)).map {
        case a :: b :: Nil => (a.asInstanceOf[T], b.asInstanceOf[S])
        case _             => fail("Sequence returned less than two elements")
      }

    def failed[T](exception: Throwable): IO[T, Effect] = fromTry(Failure(exception))

    def successful[T](result: T): IO[T, Effect] = fromTry(Success(result))

    def apply[T](body: => T): IO[T, Effect] = fromTry(Try(body))

    def foldLeft[T, R, E <: Effect](ios: collection.immutable.Iterable[IO[T, E]])(zero: R)(op: (R, T) => R): IO[R, E] =
      sequence(ios).map(_.foldLeft(zero)(op))

    def reduceLeft[T, R >: T, E <: Effect](ios: collection.immutable.Iterable[IO[T, E]])(op: (R, T) => R): IO[R, E] =
      sequence(ios).map(_.reduceLeft(op))

    def traverse[A, B, M[X] <: TraversableOnce[X], E <: Effect](in: M[A])(fn: A => IO[B, E])(implicit cbf: CanBuildFrom[M[A], B, M[B]]): IO[M[B], E] =
      sequence(in.map(fn)).map(r => cbf().++=(r).result)
  }

  sealed trait IO[+T, -E <: Effect] {

    def transactional: IO[T, E with Effect.Transaction] = Transactional(this)

    def transformWith[S, E2 <: Effect](f: Try[T] => IO[S, E2]): IO[S, E with E2] =
      TransformWith(this, f)

    def transform[S](f: Try[T] => Try[S]): IO[S, E] =
      transformWith { r =>
        IO.fromTry(f(r))
      }

    def lowerFromTry[U](implicit ev: T => Try[U]) =
      transform(_.flatMap(ev))

    def liftToTry: IO[Try[T], E] =
      transformWith(IO.successful)

    def failed: IO[Throwable, E] =
      transform {
        case Failure(t) => Success(t)
        case Success(_) => Failure(new NoSuchElementException("IO.failed not completed with a throwable."))
      }

    def map[S](f: T => S): IO[S, E] = transform(_.map(f))

    def flatMap[S, E2 <: Effect](f: T => IO[S, E2]): IO[S, E with E2] =
      transformWith {
        case Success(s)     => f(s)
        case f @ Failure(_) => IO.fromTry[S](f.asInstanceOf[Failure[S]]).asInstanceOf[IO[S, E with E2]]
      }

    def filter(p: T => Boolean): IO[T, E] =
      map { r => if (p(r)) r else throw new NoSuchElementException("IO.filter predicate is not satisfied") }

    final def withFilter(p: T => Boolean): IO[T, E] = filter(p)

    def collect[S](pf: PartialFunction[T, S]): IO[S, E] =
      map {
        r => pf.applyOrElse(r, (t: T) => throw new NoSuchElementException("IO.collect partial function is not defined at: " + t))
      }

    def recover[U >: T](pf: PartialFunction[Throwable, U]): IO[U, E] =
      transform { _ recover pf }

    def recoverWith[U >: T, E2 <: Effect](pf: PartialFunction[Throwable, IO[U, E2]]): IO[U, E with E2] =
      transformWith {
        case Failure(t)     => pf.applyOrElse(t, IO.failed _)
        case s @ Success(_) => IO.fromTry(s)
      }

    def zip[S, E2 <: Effect](io: IO[S, E2]): IO[(T, S), E with E2] =
      IO.zip(this, io)
  }
}
