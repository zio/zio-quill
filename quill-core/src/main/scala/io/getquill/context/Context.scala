package io.getquill.context

import java.io.Closeable

import scala.reflect.ClassTag
import scala.util.DynamicVariable

import io.getquill.dsl.CoreDsl

abstract class Context[R: ClassTag, S: ClassTag] extends Closeable with CoreDsl {

  type Decoder[T] = io.getquill.context.Decoder[R, T]
  type Encoder[T] = io.getquill.context.Encoder[S, T]

  implicit def mappedDecoder[I, O](implicit mapped: MappedEncoding[I, O], decoder: Decoder[I]): Decoder[O] =
    new Decoder[O] {
      def apply(index: Int, row: R) =
        mapped.f(decoder(index, row))
    }

  implicit def mappedEncoder[I, O](implicit mapped: MappedEncoding[I, O], encoder: Encoder[O]): Encoder[I] =
    new Encoder[I] {
      def apply(index: Int, value: I, row: S) =
        encoder(index, mapped.f(value), row)
    }

  implicit def wrappedTypeDecoder[T <: WrappedType] =
    MappedEncoding[T, T#Type](_.value)

  protected def handleSingleResult[T](list: List[T]) =
    list match {
      case value :: Nil => value
      case other        => throw new IllegalStateException(s"Expected a single result but got $other")
    }
}

object Context {

  private[getquill] val configPrefix = new DynamicVariable[Option[String]](None)
}
