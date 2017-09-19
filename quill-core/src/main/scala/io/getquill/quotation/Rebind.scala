package io.getquill.quotation

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Ast
import io.getquill.ast.Function
import io.getquill.ast.FunctionApply
import io.getquill.ast.Ident
import io.getquill.ast.QuotedReference

object Rebind {

  def apply(c: Context)(tree: c.Tree, ast: Ast, astParser: c.Tree => Ast): Option[Ast] = {
    import c.universe.{ Function => _, Ident => _, _ }

    def toIdent(s: Symbol) =
      Ident(s.name.decodedName.toString)

    def paramIdents(method: MethodSymbol) =
      method.paramLists.flatten.map(toIdent)

    tree match {
      case q"$conv($orig).$m[..$t](...$params)" =>
        val convMethod = conv.symbol.asMethod
        val origIdent = paramIdents(convMethod).head
        val paramsIdents = paramIdents(convMethod.returnType.member(m).asMethod)
        val paramsAsts = params.flatten.map(astParser)
        val function = QuotedReference(tree, Function(origIdent :: paramsIdents, ast))
        val apply = FunctionApply(function, astParser(orig) :: paramsAsts)
        Some(apply)
      case _ =>
        None
    }
  }
}
