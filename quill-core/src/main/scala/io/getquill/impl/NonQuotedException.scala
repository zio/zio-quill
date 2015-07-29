package io.getquill.impl

object NonQuotedException {

  def apply() = throw new IllegalStateException("The query definition must happen inside a `quote` block.")
}