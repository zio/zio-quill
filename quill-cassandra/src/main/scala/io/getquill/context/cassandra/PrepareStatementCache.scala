package io.getquill.context.cassandra

import java.util.concurrent.Callable

import com.datastax.driver.core.{ BoundStatement, PreparedStatement }
import com.google.common.base.Charsets
import com.google.common.cache.CacheBuilder
import com.google.common.hash.Hashing

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Success

class PrepareStatementCache(size: Long) {

  private val cache =
    CacheBuilder
      .newBuilder
      .maximumSize(size)
      .build[java.lang.Long, PreparedStatement]

  private val hasher = Hashing.goodFastHash(128)

  def apply(stmt: String)(prepare: String => PreparedStatement): BoundStatement =
    cache.get(
      hash(stmt),
      new Callable[PreparedStatement] {
        override def call = prepare(stmt)
      }
    ).bind

  def async(stmt: String)(prepare: String => Future[PreparedStatement])(implicit context: ExecutionContext): Future[BoundStatement] = {
    val key = hash(stmt)
    val found = cache.getIfPresent(key)

    if (found != null) Future.successful(found.bind)
    else prepare(stmt).andThen {
      case Success(s) => cache.put(key, s)
    }.map(_.bind())
  }

  private def hash(string: String): java.lang.Long = {
    hasher
      .hashString(string, Charsets.UTF_8)
      .asLong()
  }

}
