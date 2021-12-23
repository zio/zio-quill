package io.getquill.norm.capture

import io.getquill.ast.Ident

object TemporaryIdent {
  def unapply(id: Ident): Option[Ident] =
    if (id.name.matches("\\[tmp_[0-9A-Za-z]+\\]"))
      Some(id)
    else
      None
}
