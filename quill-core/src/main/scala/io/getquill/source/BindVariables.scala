package io.getquill.source

import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.ast.StatefulTransformer

private[source] case class BindVariables(state: (List[Ident], List[Ident]))
    extends StatefulTransformer[(List[Ident], List[Ident])] {

  override def apply(ast: Ast) =
    (state, ast) match {
      case ((vars, bindings), i: Ident) if (vars.contains(i)) =>
        (Ident("?"), BindVariables((vars, bindings :+ i)))
      case other => super.apply(ast)
    }
}

private[source] object BindVariables {

  def apply(ast: Ast, idents: List[Ident]) =
    (new BindVariables((idents, List()))(ast)) match {
      case (ast, transformer) =>
        transformer.state match {
          case (_, bindings) => (ast, bindings)
        }
    }
}
