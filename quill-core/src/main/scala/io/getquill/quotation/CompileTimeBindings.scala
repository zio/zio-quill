package io.getquill.quotation

import io.getquill.ast.{ Ast, CompileTimeBinding, StatefulTransformer }

import scala.reflect.macros.whitebox.Context

case class CompileTimeBindings[T](state: List[CompileTimeBinding[T]])
  extends StatefulTransformer[List[CompileTimeBinding[T]]] {

  override def apply(ast: Ast): (Ast, StatefulTransformer[List[CompileTimeBinding[T]]]) =
    ast match {
      case binding: CompileTimeBinding[T] => (binding, CompileTimeBindings(binding :: state))
      case other =>
        super.apply(other)
    }
}

object CompileTimeBindings {
  def apply(c: Context)(ast: Ast): List[CompileTimeBinding[c.Tree]] =
    new CompileTimeBindings[c.Tree](List.empty[CompileTimeBinding[c.Tree]])(ast) match {
      case (_, transformer) =>
        transformer.state.reverse
    }
}
