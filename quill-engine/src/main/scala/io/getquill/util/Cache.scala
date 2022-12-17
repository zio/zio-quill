package io.getquill.util

import java.io.Closeable
import java.lang.System.{ currentTimeMillis => now }

import scala.concurrent.duration.Duration

class Cache[K, V <: Closeable] {

  private case class Entry(value: Option[V], expiration: Long)

  private var cache = Map[K, Entry]()

  def getOrElseUpdate(key: K, value: => Option[V], ttl: Duration): Option[V] =
    synchronized {
      evict()
      val expiration = now + ttl.toMillis
      cache.get(key) match {
        case Some(entry) =>
          cache += key -> entry.copy(expiration = expiration)
          entry.value
        case None =>
          val v = value
          cache += key -> Entry(v, expiration)
          v
      }
    }

  private def evict() =
    for ((key, Entry(value, expiration)) <- cache)
      if (now > expiration) {
        value.map(_.close)
        cache -= key
      }
}
