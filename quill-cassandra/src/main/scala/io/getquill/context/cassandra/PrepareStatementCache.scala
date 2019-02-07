package io.getquill.context.cassandra

import java.util.concurrent.Callable

import com.datastax.driver.core.{BoundStatement, PreparedStatement}
import com.google.common.base.Charsets
import com.google.common.cache.CacheBuilder
import com.google.common.hash.Hashing

import scala.language.higherKinds
import scala.util.Success

class PrepareStatementCache[F[_], EC](size: Long, val contextEffect: CassandraContextEffect[F, EC]) {

  val withContextActions = contextEffect.withContextActions
  import withContextActions._
  import contextEffect.ImplicitsWithContext._

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

  def async(stmt: String)(prepare: String => F[PreparedStatement])(implicit context: EC): F[BoundStatement] = {
    val key = hash(stmt)
    val found = cache.getIfPresent(key)

    if (found != null) wrap(found.bind)
    else
      tryAndThen(prepare(stmt)) {
        case Success(s) => cache.put(key, s)
      }.map(_.bind())
  }

  private def hash(string: String): java.lang.Long = {
    hasher
      .hashString(string, Charsets.UTF_8)
      .asLong()
  }

}
