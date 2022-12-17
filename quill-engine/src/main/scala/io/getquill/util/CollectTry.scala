package io.getquill.util

import scala.util.Try

object CollectTry {
  def apply[T](list: List[Try[T]]): Try[List[T]] =
    list.foldLeft(Try(List.empty[T])) {
      case (list, t) =>
        list.flatMap { l =>
          t.map(l :+ _)
        }
    }
}