package io.getquill.codegen.util
import scala.util.{Failure, Try}

object TryOps {
  implicit final class TryThrowExt[T](private val t: Try[T]) extends AnyVal {
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
