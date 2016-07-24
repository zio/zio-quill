package io.getquill.context.mirror

import scala.reflect.macros.whitebox.{ Context => MacroContext }

import io.getquill.MirrorContext
import io.getquill.ast.{ Ast, CollectAst, Ident, Returning }
import io.getquill.context.ContextMacro
import io.getquill.norm.Normalize
import io.getquill.quotation.IsDynamic
import io.getquill.util.Messages.RichContext

class MirrorContextMacro(val c: MacroContext) extends ContextMacro {
  import c.universe.{ Ident => _, _ }

  override protected def prepare(ast: Ast, params: List[Ident]) =
    IsDynamic(ast) match {
      case false =>
        val returning = CollectAst(ast) {
          case Returning(_, property) => property
        }.headOption

        val normalized = Normalize(ast)

        probeQuery[MirrorContext](_.probe(normalized))
        c.info(normalized.toString)

        q"($normalized, $params, $returning)"
      case true =>
        q"""
          import io.getquill.norm._
          import io.getquill.ast._

          val ast = ${ast: Ast}: Ast

          val returning = CollectAst(ast) {
            case Returning(_, property) => property
          }.headOption

          val normalized = Normalize(ast)

          (normalized, $params, returning)
        """
    }
}
