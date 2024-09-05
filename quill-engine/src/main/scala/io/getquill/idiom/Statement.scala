package io.getquill.idiom

import io.getquill.ast._

sealed trait Token    extends Product with Serializable
sealed trait TagToken extends Token

final case class StringToken(string: String) extends Token {
  override def toString: String = string
}

final case class ScalarTagToken(tag: ScalarTag) extends TagToken {
  override def toString: String = s"lift(${tag.uid})"
}

final case class QuotationTagToken(tag: QuotationTag) extends TagToken {
  override def toString: String = s"quoted(${tag.uid})"
}

final case class ScalarLiftToken(lift: ScalarLift) extends Token {
  override def toString: String = s"lift(${lift.name})"
}

final case class ValuesClauseToken(statement: Statement) extends Token {
  override def toString: String = statement.toString
}

final case class Statement(tokens: List[Token]) extends Token {
  override def toString: String = tokens.mkString
}

final case class SetContainsToken(a: Token, op: Token, b: Token) extends Token {
  override def toString: String = s"${a.toString} ${op.toString} (${b.toString})"
}
