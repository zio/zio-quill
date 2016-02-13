package io.getquill.sources

import scala.reflect.macros.whitebox.Context
import io.getquill._
import io.getquill.ast.{ Query => _, Action => _, _ }
import io.getquill.norm.Normalize
import io.getquill.quotation.Quotation
import io.getquill.quotation.Quoted
import io.getquill.util.Messages.RichContext
import io.getquill.quotation.FreeVariables

trait SourceMacro extends Quotation with ActionMacro with QueryMacro with ResolveSourceMacro {
  val c: Context
  import c.universe.{ Function => _, Ident => _, _ }

  protected def prepare(ast: Ast, params: List[Ident]): Tree

  def run[R, S](quoted: Expr[Quoted[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S]): Tree = {
    implicit val t = c.WeakTypeTag(quoted.actualType.baseType(c.weakTypeOf[Quoted[Any]].typeSymbol).typeArgs.head)
    verifyFreeVariables(ast(quoted)) match {

      case ast if (t.tpe.typeSymbol.fullName.startsWith("scala.Function")) =>
        val bodyType = c.WeakTypeTag(t.tpe.typeArgs.takeRight(1).head)
        val params = (1 until t.tpe.typeArgs.size).map(i => Ident(s"p$i")).toList
        run(FunctionApply(ast, params), params.zip(paramsTypes(t)))(r, s, bodyType)

      case ast =>
        run(ast, Nil)(r, s, t)
    }
  }

  private def run[R, S, T](ast: Ast, params: List[(Ident, Type)])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    ast match {
      case ast if ((t.tpe.erasure <:< c.weakTypeTag[Action[Any]].tpe.erasure)) =>
        runAction[S, T](ast, params)

      case ast =>
        runQuery(ast, params)(r, s, queryType(t.tpe))
    }

  private def queryType(tpe: Type) =
    if (tpe <:< c.typeOf[Query[_]])
      c.WeakTypeTag(tpe.baseType(c.typeOf[Query[_]].typeSymbol).typeArgs.head)
    else
      c.WeakTypeTag(tpe)

  private def ast[T](quoted: Expr[Quoted[T]]) =
    unquote[Ast](quoted.tree).getOrElse {
      Dynamic(quoted.tree)
    }

  private def paramsTypes[T](implicit t: WeakTypeTag[T]) =
    t.tpe.typeArgs.dropRight(1)

  private def verifyFreeVariables(ast: Ast) =
    FreeVariables(ast).toList match {
      case Nil  => ast
      case vars => c.fail(s"A quotation must not have references to variables outside its scope. Found: '${vars.mkString(", ")}' in '$ast'.")
    }
}
