package io.getquill.codegen.util
import scala.util.{Failure, Success, Try}

object OptionOps {
  implicit final class OptExt[T](private val o: Option[T]) extends AnyVal {
    def mapIfThen(`if`: T, `then`: T): Option[T] = o.map(v => if (v == `if`) `then` else v)
    def toTry(e: Throwable): Try[T] = o match {
      case Some(value) => Success(value)
      case None        => Failure(e)
    }
  }
  implicit final class StringOptionExt(private val opt: Option[String]) extends AnyVal {
    def andNotEmpty: Option[String] = opt.flatMap(s => if (s.trim.isEmpty) None else Some(s))
  }
}
