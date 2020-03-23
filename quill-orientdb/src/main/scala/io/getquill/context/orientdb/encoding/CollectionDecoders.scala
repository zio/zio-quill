package io.getquill.context.orientdb.encoding

import io.getquill.context.orientdb.OrientDBSessionContext
import scala.jdk.CollectionConverters._
import com.orientechnologies.orient.core.db.record.OTrackedSet

trait CollectionDecoders {
  this: OrientDBSessionContext[_] =>

  implicit def listDecoder[T]: Decoder[List[T]] = decoder((index, row) => {
    row.field[java.util.List[T]](row.fieldNames()(index)).asScala.toList
  })
  implicit def setDecoder[T]: Decoder[Set[T]] = decoder((index, row) => {
    row.field[OTrackedSet[T]](row.fieldNames()(index)).asScala.toSet
  })
  implicit def mapDecoder[K, V]: Decoder[Map[K, V]] = decoder((index, row) => {
    row.field[java.util.Map[K, V]](row.fieldNames()(index)).asScala.toMap[K, V]
  })
}