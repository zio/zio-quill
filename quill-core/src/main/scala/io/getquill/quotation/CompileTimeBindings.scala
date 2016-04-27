package io.getquill.quotation

import io.getquill.ast.{ Ast, CompileTimeBinding, StatefulTransformer }

import scala.reflect.macros.whitebox.Context

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
