package io.getquill.context.cassandra

import java.util.concurrent.Callable

import com.google.common.base.Charsets
import com.google.common.cache.CacheBuilder
import com.google.common.hash.Hashing

class PrepareStatementCache[V <: AnyRef](size: Long) {

  private val cache =
    CacheBuilder
      .newBuilder
      .maximumSize(size)
      .build[java.lang.Long, V]()

  private val hasher = Hashing.goodFastHash(128)

  def apply(stmt: String)(prepare: String => V): V = {
    cache.get(
      hash(stmt),
      new Callable[V] {
        override def call: V = prepare(stmt)
      }
    )
  }

  def invalidate(stmt: String): Unit = cache.invalidate(hash(stmt))

  private def hash(string: String): java.lang.Long = {
    hasher
      .hashString(string, Charsets.UTF_8)
      .asLong()
  }

}
