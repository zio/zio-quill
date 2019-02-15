package io.getquill.context.cassandra

import com.google.common.util.concurrent.ListenableFuture

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

// TODO Need to implement
class CassandraFutureContextEffect extends CassandraContextEffect[Future, ExecutionContext] {

  override def wrap[T](t: => T): Future[T] = ???
  override def push[A, B](result: Future[A])(f: A => B): Future[B] = ???
  override def flatPush[A, B](result: Future[A])(f: A => Future[B]): Future[B] = ???
  override def seq[A, B](f: List[Future[A]]): Future[List[A]] = ???

  override val executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def withContextActions: WithContextActions = new WithContextActions {
    override def wrapListenableFuture[T](listenableFuture: ListenableFuture[T])(implicit executionContext: ExecutionContext): Future[T] = ???
    override def tryAndThen[T, U](t: => Future[T])(pf: PartialFunction[Try[T], U])(implicit executionContext: ExecutionContext): Future[T] = ???
    override def wrap[T](t: => T)(implicit executionContext: ExecutionContext): Future[T] = ???
    override def push[A, B](result: Future[A])(f: A => B)(implicit executionContext: ExecutionContext): Future[B] = ???
    override def flatPush[A, B](result: Future[A])(f: A => Future[B])(implicit executionContext: ExecutionContext): Future[B] = ???
    override def seq[A, B](f: List[Future[A]])(implicit executionContext: ExecutionContext): Future[List[A]] = ???
  }
}
