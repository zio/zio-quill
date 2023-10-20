package io.getquill.util

object Show {
  trait Show[T] {
    def show(v: T): String
  }

  object Show {
    // noinspection ConvertExpressionToSAM
    def apply[T](f: T => String): Show[T] =
      new Show[T] {
        def show(v: T): String = f(v)
      }
  }

  implicit final class Shower[T](private val v: T) extends AnyVal {
    def show(implicit shower: Show[T]): String = shower.show(v)
  }

  implicit def listShow[T](implicit shower: Show[T]): Show[List[T]] =
    Show[List[T]] { list =>
      list.map(_.show).mkString(", ")
    }
}
