package zio.quill.context.sql.norm

import zio.quill.ast._

object ExpandMappedInfix {
  def apply(q: Ast): Ast =
    Transform(q) { case Map(Infix("" :: parts, (q: Query) :: params, pure, tr, quat), x, p) =>
      Infix("" :: parts, Map(q, x, p) :: params, pure, tr, quat)
    }
}
