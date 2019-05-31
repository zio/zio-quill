package io.getquill.norm

trait EqualityBehavior
object EqualityBehavior {
  case object AnsiEquality extends EqualityBehavior
  case object NonAnsiEquality extends EqualityBehavior
}

