package io.getquill.source.cassandra

import java.util.concurrent.Callable
import com.datastax.driver.core.BatchStatement
import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.PreparedStatement
import com.datastax.driver.core.Statement
import com.google.common.cache.CacheBuilder
import com.datastax.driver.core.ConsistencyLevel

class PrepareStatement(prepare: String => PreparedStatement, size: Long) {

  private val cache =
    CacheBuilder
      .newBuilder
      .maximumSize(size)
      .build[java.lang.Long, PreparedStatement]

  def apply(stmt: String) =
    cache.get(stmt.hashCode,
      new Callable[PreparedStatement] {
        override def call = prepare(stmt)
      }).bind

  def apply(stmt: String, bind: BoundStatement => BoundStatement): Statement =
    bind(apply(stmt))
}
