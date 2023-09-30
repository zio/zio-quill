package io.getquill.util

import java.io.Closeable
import scala.collection.mutable
import scala.concurrent.duration.Duration

final class Cache[K, V <: Closeable] {

  private case class Entry(value: V, expiration: Long)

  private val cache = mutable.Map.empty[K, Entry]

  def getOrElseUpdate(key: K, value: => Option[V], ttl: Duration): Option[V] =
    synchronized {
      val now = System.currentTimeMillis
      evict(now)
      val expiration = now + ttl.toMillis
      cache.get(key) match {
        case Some(entry) =>
          cache += key -> entry.copy(expiration = expiration)
          Some(entry.value)
        case None =>
          val v = value
          v.foreach(vv => cache += key -> Entry(vv, expiration))
          v
      }
    }

  private def evict(now: Long): Unit =
    for ((key, Entry(value, expiration)) <- cache)
      if (now > expiration) {
        value.close()
        cache -= key
      }
}
