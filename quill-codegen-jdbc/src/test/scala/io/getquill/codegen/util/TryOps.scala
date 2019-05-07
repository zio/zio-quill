package io.getquill.codegen.util
import scala.util.{ Failure, Try }

object TryOps {
  implicit class TryThrowExt[T](t: Try[T]) {
    def orThrow: T =
      t match {
        case scala.util.Success(v) => v
        case Failure(e)            => throw e
      }

    def orThrow(wrapper: Throwable => Throwable): T =
      t match {
        case scala.util.Success(v) => v
        case Failure(e)            => throw wrapper(e)
      }
  }
}
