package io.getquill.context.mirror

import scala.reflect.ClassTag

/**
 * Defines a artificial 'Row' used for the mirror context. Mostly used for testing.
 * (Note that this must not be in quill-engine or it will conflict with the io.getquill.context.mirror.Row
 * class in ProtoQuill.)
 */
case class Row private (data: List[Any]) {
  // Nulls need a special placeholder so they can be checked via `nullAt`.
  def add(value: Any) =
    value match {
      case null => new Row((data :+ null))
      case _    => new Row((data :+ value))
    }

  def nullAt(index: Int): Boolean = data.apply(index) == null
  def apply[T](index: Int)(implicit t: ClassTag[T]) =
    data(index) match {
      case v: T  => v
      case other => throw new IllegalStateException(s"Invalid column type. Expected '${t.runtimeClass}', but got '$other'")
    }
}

object Row {
  def apply(data: Any*) =
    data.foldLeft(new Row(List()))((r, value) => r.add(value))
}
