package io.getquill.util

import java.io.Closeable

import scala.concurrent.duration.DurationInt

import io.getquill.Spec

class CacheSpec extends Spec {

  class Value extends Closeable {
    var closes = 0
    override def close = closes += 1
  }

  "caches hits" in {
    val cache = new Cache[Int, Value]

    val value = new Value
    var calls = 0
    def _value = {
      calls += 1
      Some(value)
    }

    cache.getOrElseUpdate(1, _value, 1.second) mustEqual Some(value)
    cache.getOrElseUpdate(1, _value, 1.second) mustEqual Some(value)

    calls mustEqual 1
  }

  "caches misses" in {
    val cache = new Cache[Int, Value]

    var calls = 0
    def _value = {
      calls += 1
      None
    }

    cache.getOrElseUpdate(1, _value, 1.second) mustEqual None
    cache.getOrElseUpdate(1, _value, 1.second) mustEqual None

    calls mustEqual 1
  }

  "expires keys" in {
    val cache = new Cache[Int, Value]

    val value = new Value
    var calls = 0
    def _value = {
      calls += 1
      Some(value)
    }

    cache.getOrElseUpdate(1, _value, -1.milliseconds)
    cache.getOrElseUpdate(2, None, -1.milliseconds)

    calls mustEqual 1
    value.closes mustEqual 1

    cache.getOrElseUpdate(1, _value, 1.second)

    calls mustEqual 2
    value.closes mustEqual 1
  }
}
