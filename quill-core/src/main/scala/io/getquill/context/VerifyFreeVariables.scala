package io.getquill.context

import scala.reflect.macros.whitebox.{Context => MacroContext}
import io.getquill.quotation.FreeVariables
import io.getquill.ast.Ast
import io.getquill.util.MacroContextExt._

object VerifyFreeVariables {

  def apply(c: MacroContext)(ast: Ast): Ast =
    FreeVariables.verify(ast) match {
      case Right(ast) => ast
      case Left(msg)  => c.fail(msg)
    }
}
