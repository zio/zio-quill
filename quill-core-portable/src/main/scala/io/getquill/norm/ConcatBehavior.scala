package io.getquill.norm

trait ConcatBehavior
object ConcatBehavior {
  case object AnsiConcat extends ConcatBehavior
  case object NonAnsiConcat extends ConcatBehavior
}
