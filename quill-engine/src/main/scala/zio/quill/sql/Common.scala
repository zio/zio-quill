package zio.quill.sql

import zio.quill.ast.{Aggregation, Ast, CollectAst, Infix}

object Common {
  object ContainsImpurities {
    def unapply(ast: Ast) =
      CollectAst(ast) {
        case agg: Aggregation          => agg
        case inf: Infix if (!inf.pure) => inf
      }.nonEmpty
  }
}
