package zio.quill.log

import zio.quill.util.ContextLogger

object ContextLog {
  private val logger = ContextLogger(this.getClass)

  def apply(str: String): Unit =
    logger.underlying.error(str)
}
