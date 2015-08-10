package io.getquill.impl

class NonQuotedException extends Exception("The query definition must happen inside a `quote` block.")

object NonQuotedException {

  def apply() = throw new NonQuotedException
}