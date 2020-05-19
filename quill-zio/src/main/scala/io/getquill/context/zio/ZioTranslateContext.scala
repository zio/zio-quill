package io.getquill.context.zio

import io.getquill.NamingStrategy
import io.getquill.context.{ Context, ContextEffect, TranslateContextBase }
import io.getquill.idiom.Idiom
import zio.{ Has, RIO }
import zio.blocking.Blocking

trait ZioTranslateContext extends TranslateContextBase {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  override type TranslateResult[T] = RIO[Has[Session] with Blocking, T]

  override private[getquill] val translateEffect: ContextEffect[TranslateResult] = new ContextEffect[TranslateResult] {
    override def wrap[T](t: => T): TranslateResult[T] = RIO.environment[Has[Session] with Blocking].as(t)
    override def push[A, B](result: TranslateResult[A])(f: A => B): TranslateResult[B] = result.map(f)
    override def seq[A](list: List[TranslateResult[A]]): TranslateResult[List[A]] = RIO.collectAll(list)
  }
}
