package io.getquill.monad

import scala.language.higherKinds
import io.getquill.context.Context
import scala.annotation.tailrec
import scala.util.Try

trait SyncIOMonad extends IOMonad {
  this: Context[_, _] =>

  type Result[T] = T

  def performIO[T](io: IO[T, _], transactional: Boolean = false): Result[T] = {

    @tailrec def loop[U](io: IO[U, _]): Result[U] = {

      def flatten[Y, M[X] <: TraversableOnce[X]](seq: Sequence[Y, M, Effect]) =
        seq.in.foldLeft(IO.successful(seq.cbfResultToValue())) {
          (builder, item) =>
            builder.flatMap(b => item.map(b += _))
        }.map(_.result())

      io match {
        case FromTry(v)              => v.get
        case Run(f)                  => f()
        case seq @ Sequence(_, _, _) => loop(flatten(seq))
        case TransformWith(a, fA) =>
          a match {
            case FromTry(v)              => loop(fA(v))
            case Run(r)                  => loop(fA(Try(r())))
            case seq @ Sequence(_, _, _) => loop(flatten(seq).transformWith(fA))
            case TransformWith(b, fB)    => loop(b.transformWith(fB(_).transformWith(fA)))
            case Transactional(io)       => loop(fA(Try(performIO(io, transactional = true))))
          }
        case Transactional(io) => performIO(io, transactional = true)
      }
    }
    loop(io)
  }
}
