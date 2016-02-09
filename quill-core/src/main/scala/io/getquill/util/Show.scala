package io.getquill.util

object Show {
  trait Show[T] {
    def show(v: T): String
  }

  object Show {
    def apply[T](f: T => String) = new Show[T] {
      def show(v: T) = f(v)
    }
  }

  implicit class Shower[T](v: T)(implicit shower: Show[T]) {
    def show = shower.show(v)
  }

  implicit def listShow[T](implicit shower: Show[T]) = Show[List[T]] {
    case list => list.map(_.show).mkString(", ")
  }
}
