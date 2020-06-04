package io.getquill.quotation

import io.getquill.quat.Quat

class QuatException(message: String) extends IllegalArgumentException(message)

object QuatExceptionOps {
  implicit class QuatExceptionOpsExt(quat: => Quat) {
    def suppress(additionalMessage: String = "") =
      try { quat.shortString } catch {
        case e: QuatException =>
          s"QuatException(${e.getMessage + (if (additionalMessage != "") ", " + additionalMessage else "")})"
      }
  }
}
