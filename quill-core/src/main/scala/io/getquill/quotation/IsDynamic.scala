package io.getquill.quotation

import io.getquill.ast._

case class IsDynamic(state: Boolean)
    extends StatefulTransformer[Boolean] {

  override def apply(a: Ast) =
    a match {
      case d: Dynamic => (d, new IsDynamic(true))
      case other      => super.apply(other)
    }
}

object IsDynamic {
  def apply(a: Ast) =
    (new IsDynamic(false)(a)) match {
      case (_, transformer) =>
        transformer.state
    }
}
