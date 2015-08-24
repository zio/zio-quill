package io.getquill.source

import io.getquill.ast.Action
import io.getquill.ast.Assignment
import io.getquill.ast.BinaryOperation
import io.getquill.ast.Delete
import io.getquill.ast.Ast
import io.getquill.ast.Filter
import io.getquill.ast.FlatMap
import io.getquill.ast.Ident
import io.getquill.ast.Insert
import io.getquill.ast.Map
import io.getquill.ast.Query
import io.getquill.ast.Entity
import io.getquill.ast.Tuple
import io.getquill.ast.UnaryOperation
import io.getquill.ast.Update
import io.getquill.ast.Function
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