package io.getquill.sources.cassandra

import java.util.concurrent.Callable
import com.datastax.driver.core.BatchStatement
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Statement
import com.google.common.cache.CacheBuilder
import com.datastax.driver.core.ConsistencyLevel
import com.typesafe.config.Config

class PrepareStatementCache(size: Long) {

  private val cache =
    CacheBuilder
      .newBuilder
      .maximumSize(size)
      .build[java.lang.Long, PreparedStatement]

  def apply(stmt: String)(prepare: String => PreparedStatement) =
    cache.get(stmt.hashCode,
      new Callable[PreparedStatement] {
        override def call = prepare(stmt)
      }).bind
}
