package io.getquill.context.async

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

class ActionApply[T](f: List[T] => Future[List[Any]])(implicit ec: ExecutionContext)
  extends Function1[List[T], Future[List[Any]]] {
  def apply(params: List[T]) = f(params)
  def apply(param: T) = f(List(param)).map(_.head)
}
