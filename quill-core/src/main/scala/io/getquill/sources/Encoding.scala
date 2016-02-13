package io.getquill.sources

import scala.reflect.macros.whitebox.Context

import io.getquill.util.InferImplicitValueWithFallback

trait Decoder[R, T] {
  def apply(index: Int, row: R): T
}

trait Encoder[S, T] {
  def apply(index: Int, value: T, row: S): S
}

case class MappedEncoding[I, O](f: I => O)

object Encoding {

  def inferDecoder[R](c: Context)(tpe: c.Type)(implicit r: c.WeakTypeTag[R]) = {
    def decoderType[T](implicit t: c.WeakTypeTag[T]) = c.weakTypeTag[Decoder[R, T]]
    InferImplicitValueWithFallback(c)(decoderType(c.WeakTypeTag(tpe)).tpe, c.prefix.tree)
  }

  def inferEncoder[R](c: Context)(tpe: c.Type)(implicit r: c.WeakTypeTag[R]) = {
    def encoderType[T](implicit t: c.WeakTypeTag[T]) = c.weakTypeTag[Encoder[R, T]]
    InferImplicitValueWithFallback(c)(encoderType(c.WeakTypeTag(tpe)).tpe, c.prefix.tree)
  }
}
