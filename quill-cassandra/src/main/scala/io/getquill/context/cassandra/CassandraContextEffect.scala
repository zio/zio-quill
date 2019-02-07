package io.getquill.context.cassandra

import com.google.common.util.concurrent.ListenableFuture
import io.getquill.context.ContextEffect

import scala.util.Try
import scala.language.higherKinds

trait CassandraContextEffect[F[_], EC] extends ContextEffect[F] { self =>

  object ImplicitsWithContext {
    implicit class ResultTypeOps[A](result: F[A]) {
      def map[B](f: A => B)(implicit executionContext: EC) = withContextActions.push(result)(f)
      def flatMap[B](f: A => F[B])(implicit executionContext: EC) = withContextActions.flatPush(result)(f)
    }
  }

  val executionContext: EC
  def withContextActions: WithContextActions

  trait WithContextActions {
    def wrapListenableFuture[T](listenableFuture: ListenableFuture[T])(implicit executionContext: EC): F[T]
    def tryAndThen[T, U](t: => F[T])(pf: PartialFunction[Try[T], U])(implicit executionContext: EC): F[T]
    def wrap[T](t: => T)(implicit executionContext: EC): F[T]
    def push[A, B](result: F[A])(f: A => B)(implicit executionContext: EC): F[B]
    def flatPush[A, B](result: F[A])(f: A => F[B])(implicit executionContext: EC): F[B]
    def seq[A, B](f: List[F[A]])(implicit executionContext: EC): F[List[A]]
  }
}
