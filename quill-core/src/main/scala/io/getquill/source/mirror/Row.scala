package io.getquill.source.mirror

import io.getquill.util.Messages._
import scala.reflect.ClassTag

case class Row(data: Any*) {
  def add(value: Any) = Row((data :+ value): _*)
  def apply[T](index: Int)(implicit t: ClassTag[T]) =
    data(index) match {
      case v: T  => v
      case other => fail(s"Invalid column type. Expected '${t.runtimeClass}', but got '$other'")
    }
}
