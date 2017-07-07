package io.getquill.context.orientdb.encoding

import io.getquill.context.orientdb.OrientDBSessionContext
import scala.collection.JavaConverters._

trait CollectionEncoders {
  this: OrientDBSessionContext[_] =>

  implicit def listEncoder[T]: Encoder[List[T]] = encoder((index, value, row) => {
    row.insert(index, value.asJava); row
  })
  implicit def setEncoder[T]: Encoder[Set[T]] = encoder((index, value, row) => {
    row.insert(index, value.asJava); row
  })
  implicit def mapEncoder[K, V]: Encoder[Map[K, V]] = encoder((index, value, row) => {
    row.insert(index, mapAsJavaMapConverter[K, V](value).asJava); row
  })
}