package io.getquill.context

import scala.reflect.macros.whitebox.{Context => MacroContext}
import io.getquill.quotation.FreeVariables
import io.getquill.ast.{Ast, Ident, Pos}
import io.getquill.util.MacroContextExt._

object VerifyFreeVariables {

  def apply(c: MacroContext)(ast: Ast): Ast = {
    import c.universe.{Ident => _, _}

    FreeVariables.verify(ast) match {
      case Right(ast) => ast
      case Left(err) =>
        err.freeVars match {
          // we we have a single position from the encosing context in the same file we can actually fail
          // at the right position and point the compiler to that location since we can modify the position
          // by the `point` info that we have from our position
          case List(Ident.WithPos(_, Pos.Real(fileName, _, _, point, _))) if (c.enclosingPosition.source.path == fileName) =>
            c.failAtPoint(err.msgNoPos, point)

          case _ =>
            c.fail(err.msg)
        }

    }
  }
}
