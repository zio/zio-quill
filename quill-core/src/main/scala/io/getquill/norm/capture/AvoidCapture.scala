package io.getquill.norm.capture

import io.getquill.ast.Query

object AvoidCapture {

  def apply(q: Query): Query =
    Dealias(AvoidAliasConflict(q))
}
