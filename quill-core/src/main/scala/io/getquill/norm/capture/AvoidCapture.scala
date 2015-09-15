package io.getquill.norm.capture

import io.getquill.ast._

object AvoidCapture {

  def apply(q: Query): Query =
    Dealias(AvoidAliasConflict(q))
}
