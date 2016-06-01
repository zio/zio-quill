package io.getquill.context.mirror

import scala.reflect.macros.whitebox.{ Context => MacroContext }

import io.getquill.MirrorContext
import io.getquill.ast.Ast
import io.getquill.ast.Ident
import io.getquill.context.ContextMacro
import io.getquill.context.ExtractEntityAndInsertAction
import io.getquill.norm.Normalize
import io.getquill.quotation.IsDynamic
import io.getquill.util.Messages.RichContext

class MirrorContextMacro(val c: MacroContext) extends ContextMacro {
  import c.universe.{ Ident => _, _ }

  override protected def prepare(ast: Ast, params: List[Ident]) =
    IsDynamic(ast) match {
      case false =>
        val normalized = Normalize(ast)
        probeQuery[MirrorContext](_.probe(normalized))
        c.info(normalized.toString)
        val (entity, insert) = ExtractEntityAndInsertAction(normalized)
        val isInsert = insert.isDefined
        val generated = if (isInsert) entity.flatMap(_.generated) else None
        q"($normalized, $params, $generated)"
      case true =>
        q"""
          import io.getquill.norm._
          import io.getquill.ast._
          import io.getquill.context.ExtractEntityAndInsertAction

          val normalized = Normalize($ast: Ast)
          val (entity, insert) = ExtractEntityAndInsertAction(normalized)
          val isInsert = insert.isDefined
          val generated = if (isInsert) entity.flatMap(_.generated) else None

          (normalized, $params, generated)
        """
    }
}
