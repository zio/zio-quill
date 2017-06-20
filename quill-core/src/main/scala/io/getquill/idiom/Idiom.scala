package io.getquill.idiom

import io.getquill.ast._
import io.getquill.NamingStrategy

trait Idiom {

  def emptySetContainsToken(field: Token): Token = StringToken("FALSE")

  def liftingPlaceholder(index: Int): String

  def translate(ast: Ast)(implicit naming: NamingStrategy): (Ast, Statement)

  def prepareForProbing(string: String): String
}
