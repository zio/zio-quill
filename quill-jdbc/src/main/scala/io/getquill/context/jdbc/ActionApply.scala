package io.getquill.context.jdbc

class ActionApply[T](f: List[T] => List[Long]) extends Function1[List[T], List[Long]] {
  def apply(params: List[T]) = f(params)
  def apply(param: T) = f(List(param)).head
}