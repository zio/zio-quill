package io.getquill.context.qzio

import io.getquill.NamingStrategy
import io.getquill.context.{ Context, ContextEffect, TranslateContextBase }
import io.getquill.idiom.Idiom
import zio.{ Has, ZIO }
import zio.blocking.Blocking

trait ZioTranslateContext extends TranslateContextBase {
  this: Context[_ <: Idiom, _ <: NamingStrategy] =>

  type Error
  type Environment

  override type TranslateResult[T] = ZIO[Environment, Error, T]

  override private[getquill] val translateEffect: ContextEffect[TranslateResult] = new ContextEffect[TranslateResult] {
    override def wrap[T](t: => T): TranslateResult[T] = ZIO.environment[Environment].as(t)
    override def push[A, B](result: TranslateResult[A])(f: A => B): TranslateResult[B] = result.map(f)
    override def seq[A](list: List[TranslateResult[A]]): TranslateResult[List[A]] = ZIO.collectAll(list)
  }
}
