package io.getquill.context

import scala.reflect.macros.whitebox.{ Context => MacroContext }

import io.getquill.util.InferImplicitValueWithFallback

trait Decoder[R, T] {
  def apply(index: Int, row: R): T
}

trait Encoder[S, -T] {
  def apply(index: Int, value: T, row: S): S
}

object Encoding {

  def inferDecoder[R](c: MacroContext)(tpe: c.Type)(implicit r: c.WeakTypeTag[R]) = {
    def decoderType[T](implicit t: c.WeakTypeTag[T]) = c.weakTypeTag[Decoder[R, T]]
    InferImplicitValueWithFallback(c)(decoderType(c.WeakTypeTag(tpe)).tpe, c.prefix.tree)
  }

  def inferEncoder[R](c: MacroContext)(tpe: c.Type)(implicit r: c.WeakTypeTag[R]) = {
    def encoderType[T](implicit t: c.WeakTypeTag[T]) = c.weakTypeTag[Encoder[R, T]]
    InferImplicitValueWithFallback(c)(encoderType(c.WeakTypeTag(tpe)).tpe, c.prefix.tree)
  }
}
