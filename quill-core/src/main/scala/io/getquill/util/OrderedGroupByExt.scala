package io.getquill.util

import scala.collection.immutable.ListSet
import scala.collection.mutable
import scala.collection.mutable.{LinkedHashMap => MMap}

object OrderedGroupByExt {
  implicit final class GroupByOrderedImplicitImpl[A](private val t: Iterable[A]) extends AnyVal {
    def groupByOrderedUnique[K](f: A => K): Map[K, ListSet[A]] =
      groupByGen(ListSet.newBuilder[A])(f)

    def groupByOrdered[K](f: A => K): Map[K, List[A]] =
      groupByGen(List.newBuilder[A])(f)

    def groupByGen[K, C[_]](makeBuilder: => mutable.Builder[A, C[A]])(f: A => K): Map[K, C[A]] = {
      val map = MMap[K, mutable.Builder[A, C[A]]]()
      for (i <- t) {
        val key = f(i)
        val builder = map.get(key) match {
          case Some(existing) => existing
          case None =>
            val newBuilder = makeBuilder
            map(key) = newBuilder
            newBuilder
        }
        builder += i
      }
      map.mapValues(_.result).toMap
    }
  }
}
