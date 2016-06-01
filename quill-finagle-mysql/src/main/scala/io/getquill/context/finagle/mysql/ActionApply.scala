package io.getquill.context.finagle.mysql

import com.twitter.util.Future

class ActionApply[T](f: List[T] => Future[List[Long]])
  extends Function1[List[T], Future[List[Long]]] {
  def apply(params: List[T]) = f(params)
  def apply(param: T) = f(List(param)).map(_.head)
}
