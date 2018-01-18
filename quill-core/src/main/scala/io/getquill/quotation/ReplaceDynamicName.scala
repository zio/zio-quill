package io.getquill.quotation

import io.getquill.ast._

object ReplaceDynamicName {
  def apply(ast: Ast) = Transform(ast) {
    case Entity(DynamicName(name: String), props) => Entity(StaticName(name), props)
  }
}
