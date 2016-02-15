package io.getquill.quotation

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.norm.BetaReduction

object Rebind {

  def apply(c: Context)(tree: c.Tree, ast: Ast, astParser: c.Tree => Ast) = {
    import c.universe.{ Ident => _, _ }

    def toIdent(s: Symbol) =
      Ident(s.name.decodedName.toString)

    def origRebind(conv: Tree, orig: Tree, astParser: Tree => Ast) = {
      val paramSymbol = conv.symbol.asMethod.paramLists.flatten.head
      (toIdent(paramSymbol) -> astParser(orig))
    }

    tree match {
      case q"$conv($orig).$method[..$t]" =>
        BetaReduction(ast, origRebind(conv, orig, astParser))
      case q"$conv($orig).$m(...$params)" =>
        val method =
          conv.symbol.asMethod.returnType.member(m)
        val paramsIdents =
          method.asMethod.paramLists.flatten.map(toIdent)
        val paramsRebind =
          paramsIdents.zip(params.flatten.map(astParser))
        BetaReduction(ast, (origRebind(conv, orig, astParser) +: paramsRebind): _*)
      case other => ast
    }
  }
}
