package zio.quill.context

import scala.reflect.macros.whitebox.{Context => MacroContext}
import zio.quill.quotation.FreeVariables
import zio.quill.ast.Ast
import zio.quill.util.MacroContextExt._

object VerifyFreeVariables {

  def apply(c: MacroContext)(ast: Ast): Ast =
    FreeVariables.verify(ast) match {
      case Right(ast) => ast
      case Left(msg)  => c.fail(msg)
    }
}
