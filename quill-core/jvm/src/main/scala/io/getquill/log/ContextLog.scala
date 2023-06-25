package io.getquill.log

import io.getquill.util.ContextLogger

object ContextLog {
  private val logger = ContextLogger(this.getClass)

  def apply(str: String) =
    logger.underlying.error(str)
}
