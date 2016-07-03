package io.getquill.quotation

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Ast
import io.getquill.ast.CompileTimeBinding
import io.getquill.ast.StatefulTransformer

case class CompileTimeBindings[T](state: List[CompileTimeBinding])
  extends StatefulTransformer[List[CompileTimeBinding]] {

  override def apply(ast: Ast): (Ast, StatefulTransformer[List[CompileTimeBinding]]) =
    ast match {
      case binding: CompileTimeBinding => (binding, CompileTimeBindings(binding :: state))
      case other =>
        super.apply(other)
    }
}

object CompileTimeBindings {
  def apply(c: Context)(ast: Ast): List[CompileTimeBinding] =
    new CompileTimeBindings[c.Tree](List.empty[CompileTimeBinding])(ast) match {
      case (_, transformer) =>
        transformer.state.reverse
    }
}
