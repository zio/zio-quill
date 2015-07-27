package io.getquill.attach

private[attach] object Cache {

  private val cache = collection.mutable.Map[Int, Any]()

  def update(attachment: Any) =
    cache.put(attachment.hashCode, attachment)

  def getOrElseUpdate[D](hashCode: Int)(extract: => D) =
    cache.getOrElseUpdate(hashCode, extract).asInstanceOf[D]
}
