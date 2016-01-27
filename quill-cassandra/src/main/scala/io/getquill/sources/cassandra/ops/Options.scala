package io.getquill.sources.cassandra.ops

import io.getquill._

abstract class Options[A](q: A) {
  def usingTimestamp(ts: Int) = quote(infix"$q USING TIMESTAMP $ts".as[A])
  def usingTtl(ttl: Int) = quote(infix"$q USING TTL $ttl".as[A])
  def using(ts: Int, ttl: Int) = quote(infix"$q USING TIMESTAMP $ts AND TTL $ttl".as[A])
}
