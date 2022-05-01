package io.getquill.context.mirror

import scala.reflect.ClassTag

import io.getquill.util.Messages.fail

/**
 * Defines a artificial 'Row' used for the mirror context. Mostly used for testing.
 * (Note that this must not be in quill-engine or it will conflict with the io.getquill.context.mirror.Row
 * class in ProtoQuill.)
 */
case class Row(data: Any*) {
  def add(value: Any) = Row((data :+ value): _*)
  def apply[T](index: Int)(implicit t: ClassTag[T]) =
    data(index) match {
      case v: T  => v
      case other => fail(s"Invalid column type. Expected '${t.runtimeClass}', but got '$other'")
    }
}
