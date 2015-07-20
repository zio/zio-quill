package io.getquill.util

object Show {
  trait Show[T] {
    def show(v: T): String
  }

  implicit class Shower[T](v: T)(implicit shower: Show[T]) {
    def show = shower.show(v)
  }
}