package io.getquill.quotation

import io.getquill.quat.Quat

class QuatException(message: String) extends IllegalArgumentException(message)

object QuatException {
  def apply(message: String) = throw new QuatException(message)
}

object QuatExceptionOps {
  implicit final class QuatExceptionOpsExt(private val quat: => Quat) extends AnyVal {
    def suppress(additionalMessage: String = ""): String =
      try { quat.shortString }
      catch {
        case e: QuatException =>
          s"QuatException(${e.getMessage + (if (additionalMessage != "") ", " + additionalMessage else "")})"
      }
  }
}
