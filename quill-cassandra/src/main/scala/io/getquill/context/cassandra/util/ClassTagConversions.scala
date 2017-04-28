package io.getquill.context.cassandra.util

import scala.reflect.ClassTag

object ClassTagConversions {
  def asClassOf[T](implicit tag: ClassTag[T]): Class[T] = tag.runtimeClass.asInstanceOf[Class[T]]
}
