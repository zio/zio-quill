package zio.quill.quotation

import zio.quill.ast.Ast
import zio.quill.ast.CollectAst
import zio.quill.ast.Dynamic

object IsDynamic {
  def apply(a: Ast) =
    CollectAst(a) { case d: Dynamic => d }.nonEmpty
}
