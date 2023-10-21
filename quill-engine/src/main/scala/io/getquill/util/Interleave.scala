package io.getquill.util

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object Interleave {

  def apply[T](l1: List[T], l2: List[T]): List[T] =
    interleave(l1, l2, ListBuffer.empty)

  @tailrec
  private[this] def interleave[T](l1: List[T], l2: List[T], acc: ListBuffer[T]): List[T] =
    (l1, l2) match {
      case (Nil, l2)            => acc.toList ++ l2
      case (l1, Nil)            => acc.toList ++ l1
      case (h1 :: t1, h2 :: t2) => interleave(t1, t2, { acc += h1; acc += h2 })
    }
}
