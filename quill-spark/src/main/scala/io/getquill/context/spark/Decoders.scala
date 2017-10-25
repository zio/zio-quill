package io.getquill.context.spark

import io.getquill.util.Messages
import io.getquill.QuillSparkContext

trait Decoders {
  this: QuillSparkContext =>

  type Decoder[T] = BaseDecoder[T]
  type ResultRow = Unit

  implicit def dummyDecoder[T] =
    (idx: Int, row: ResultRow) => Messages.fail("quill decoders are not used for spark")

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    dummyDecoder[O]
}