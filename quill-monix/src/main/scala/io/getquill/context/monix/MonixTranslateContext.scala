package io.getquill.context.monix

import io.getquill.NamingStrategy
import io.getquill.context.{ Context, ContextEffect, TranslateContextBase }
import io.getquill.idiom.Idiom
import monix.eval.Task

trait MonixTranslateContext extends TranslateContextBase {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  override type TranslateResult[T] = Task[T]

  override private[getquill] val translateEffect: ContextEffect[Task] = new ContextEffect[Task] {
    override def wrap[T](t: => T): Task[T] = Task.eval(t)
    override def push[A, B](result: Task[A])(f: A => B): Task[B] = result.map(f)
    override def seq[A](list: List[Task[A]]): Task[List[A]] = Task.sequence(list)
  }
}
