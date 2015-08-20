package io.getquill.quotation

class NonQuotedException extends Exception("The query definition must happen inside a `quote` block.")

object NonQuotedException {

  def apply() = throw new NonQuotedException
}