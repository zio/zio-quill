package zio.quill.quotation

import zio.quill.quat.Quat

class QuatException(message: String) extends IllegalArgumentException(message)

object QuatException {
  def apply(message: String) = throw new QuatException(message)
}

object QuatExceptionOps {
  implicit final class QuatExceptionOpsExt(quat: => Quat) {
    def suppress(additionalMessage: String = ""): String =
      try { quat.shortString }
      catch {
        case e: QuatException =>
          s"QuatException(${e.getMessage + (if (additionalMessage != "") ", " + additionalMessage else "")})"
      }
  }
}
