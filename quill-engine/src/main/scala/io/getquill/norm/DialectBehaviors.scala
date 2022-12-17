package io.getquill.norm

trait EqualityBehavior
object EqualityBehavior {
  case object AnsiEquality extends EqualityBehavior
  case object NonAnsiEquality extends EqualityBehavior
}

trait ConcatBehavior
object ConcatBehavior {
  case object AnsiConcat extends ConcatBehavior
  case object NonAnsiConcat extends ConcatBehavior
}

sealed trait ProductAggregationToken
object ProductAggregationToken {
  case object Star extends ProductAggregationToken
  case object VariableDotStar extends ProductAggregationToken
}
