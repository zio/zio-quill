package io.getquill.quotation

class NonQuotedException extends Exception(NonQuotedException.message)

object NonQuotedException {
  final val message    = "The query definition must happen within a `quote` block."
  def apply(): Nothing = throw new NonQuotedException
}
