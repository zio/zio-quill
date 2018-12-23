package io.getquill.context

import scala.language.higherKinds

/**
 * In order to be able to reuse methods in the Jdbc Context as well as others, there must be a way
 * to encapsulate the effects of these contexts. This simple interface provides them in a fairly
 * generic manner.
 */
trait ContextEffect[F[_]] {
  /**
   * Lift an element or block of code in the context into the specified effect.
   */
  def wrap[T](t: => T): F[T]

  /**
   * Map a parameter of the effect. This is really just a functor.
   */
  def push[A, B](result: F[A])(f: A => B): F[B]

  /**
   * Aggregate a list of effects into a single effect element. Most effect types
   * used in Quill context easily support this kind of operation e.g. Futures, monix Tasks, Observables, etc...
   */
  def seq[A, B](f: List[F[A]]): F[List[A]]
}
