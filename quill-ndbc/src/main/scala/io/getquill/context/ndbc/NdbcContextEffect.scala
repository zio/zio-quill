package io.getquill.context.ndbc

import io.getquill.context.ContextEffect
import io.trane.future.scala.Future

trait NdbcContextEffect extends ContextEffect[Future] {
  def wrap[T](t: => T): Future[T] = Future(t)
  def push[A, B](result: Future[A])(f: A => B): Future[B] = result.map(f)
  def seq[A, B](list: List[Future[A]]): Future[List[A]] = Future.sequence(list)
}

object NdbcContextEffect extends NdbcContextEffect