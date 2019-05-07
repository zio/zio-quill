package io.getquill.codegen.util
import scala.util.{ Failure, Success, Try }

object OptionOps {
  implicit class OptExt[T](o: Option[T]) {
    def mapIfThen(`if`: T, `then`: T): Option[T] = o.map(v => if (v == `if`) `then` else v)
    def toTry(e: Throwable): Try[T] = o match {
      case Some(value) => Success(value)
      case None        => Failure(e)
    }
  }
  implicit class StringOptionExt(opt: Option[String]) {
    def andNotEmpty = opt.flatMap(s => if (s.trim.isEmpty) None else Some(s))
  }
}
