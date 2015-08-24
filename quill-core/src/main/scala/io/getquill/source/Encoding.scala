package io.getquill.source

import scala.reflect.macros.whitebox.Context

import io.getquill.util.InferImplicitValueWithFallback

trait Decoder[R, T] {
  def apply(index: Int, row: R): T
}

trait Encoder[S, T] {
  def apply(index: Int, value: T, row: S): S
}

object Encoding {

  def inferDecoder[R](c: Context)(tpe: c.Type)(implicit r: c.WeakTypeTag[R]) = {
    def decoderType[T, R](implicit t: c.WeakTypeTag[T], r: c.WeakTypeTag[R]) = c.weakTypeTag[Decoder[R, T]]
    InferImplicitValueWithFallback(c)(decoderType(c.WeakTypeTag(tpe), r).tpe, c.prefix.tree)
  }

  def inferEcoder[R](c: Context)(tpe: c.Type)(implicit r: c.WeakTypeTag[R]) = {
    def encoderType[T, R](implicit t: c.WeakTypeTag[T], r: c.WeakTypeTag[R]) = c.weakTypeTag[Encoder[R, T]]
    InferImplicitValueWithFallback(c)(encoderType(c.WeakTypeTag(tpe), r).tpe, c.prefix.tree)
  }

}
