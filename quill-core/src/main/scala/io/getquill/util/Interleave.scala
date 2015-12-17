package io.getquill.util

import scala.annotation.tailrec

object Interleave {

  def apply[T](l1: List[T], l2: List[T]): List[T] =
    interleave(l1, l2, List.empty)

  @tailrec
  private[this] def interleave[T](l1: List[T], l2: List[T], acc: List[T]): List[T] =
    (l1, l2) match {
      case (Nil, l2)            => acc ++ l2
      case (l1, Nil)            => acc ++ l1
      case (h1 :: t1, h2 :: t2) => interleave(t1, t2, acc :+ h1 :+ h2)
    }
}
