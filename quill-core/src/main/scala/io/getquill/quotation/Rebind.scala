package io.getquill.quotation

import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Ast
import io.getquill.ast.Function
import io.getquill.ast.FunctionApply
import io.getquill.ast.Ident
import io.getquill.ast.QuotedReference
import io.getquill.quat.QuatMaking

import scala.reflect.macros.whitebox

object Rebind {

  def apply(c: Context)(tree: c.Tree, ast: Ast, astParser: c.Tree => Ast): Option[Ast] = {
    import c.universe.{Function => _, Ident => _, _}
    val ctx = c
    val infer = new QuatMaking {
      override val c: whitebox.Context = ctx
    }

    def toIdent(s: Symbol) =
      // Casing there is needed because scala doesn't understand c.universe.Type =:= infer.c.universe.Type
      // alternatively, we could wrap this entire clause (starting with 'apply') in a class and extend inferQuat
      Ident(
        s.name.decodedName.toString,
        infer.inferQuat(s.typeSignature.asInstanceOf[infer.c.universe.Type])
      ) // TODO Verify Quat

    def paramIdents(method: MethodSymbol) =
      method.paramLists.flatten.map(toIdent)

    def placeholder(t: Tree): Tree =
      q"null.asInstanceOf[${t.tpe}]"

    tree match {
      case q"$conv($orig).$m[..$t](...$params)" =>
        val convMethod   = conv.symbol.asMethod
        val origIdent    = paramIdents(convMethod).head
        val paramsIdents = paramIdents(convMethod.returnType.member(m).asMethod)
        val paramsAsts   = params.flatten.map(astParser)
        val reifiedTree  = q"$conv(${placeholder(orig)}).$m[..$t](...${params.map(_.map(placeholder(_)))})"
        val function     = QuotedReference(reifiedTree, Function(origIdent :: paramsIdents, ast))
        val apply        = FunctionApply(function, astParser(orig) :: paramsAsts)
        Some(apply)
      case _ =>
        None
    }
  }
}
