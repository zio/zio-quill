package io.getquill.context.cassandra

import java.util.concurrent.Callable
import com.datastax.driver.core.PreparedStatement
import com.google.common.cache.CacheBuilder

class PrepareStatementCache(size: Long) {

  private val cache =
    CacheBuilder
      .newBuilder
      .maximumSize(size)
      .build[java.lang.Long, PreparedStatement]

  def apply(stmt: String)(prepare: String => PreparedStatement) =
    cache.get(
      stmt.hashCode,
      new Callable[PreparedStatement] {
        override def call = prepare(stmt)
      }
    ).bind
}
