package io.getquill.attach

import scala.reflect.api.Liftables
import scala.reflect.internal.Trees

private[attach] object Cache {

  private val cache = collection.mutable.Map[Int, Any]()

  def update(attachment: Any) =
    cache.put(attachment.hashCode, attachment)

  def getOrElseUpdate[D](hashCode: Int)(extract: => D) =
    cache.getOrElseUpdate(hashCode, extract).asInstanceOf[D]
}
