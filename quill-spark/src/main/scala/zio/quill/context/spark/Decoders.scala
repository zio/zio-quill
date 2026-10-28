package zio.quill.context.spark

import zio.quill.util.Messages
import zio.quill.QuillSparkContext

trait Decoders {
  this: QuillSparkContext =>

  type Decoder[T] = BaseDecoder[T]
  type ResultRow  = Unit

  implicit def dummyDecoder[T]: (Index, ResultRow, ResultRow) => Nothing =
    (_: Int, _: ResultRow, _: Session) => Messages.fail("quill decoders are not used for spark")

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    dummyDecoder[O]
}
