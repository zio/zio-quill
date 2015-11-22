package io.getquill.source

import scala.reflect.macros.whitebox.Context

import io.getquill._
import io.getquill.ast.{ Query => _, Action => _, _ }
import io.getquill.norm.Normalize
import io.getquill.quotation.Quotation
import io.getquill.quotation.Quoted
import io.getquill.util.Messages.RichContext

trait SourceMacro extends Quotation with ActionMacro with QueryMacro with ResolveSourceMacro {
  val c: Context
  import c.universe.{ Function => _, Ident => _, _ }

  protected def toExecutionTree(ast: Ast): Tree

  protected def prepare(ast: Ast, params: List[Ident]): Tree

  def run[R, S, T](quoted: Expr[Quoted[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {

    ast(quoted) match {

      case ast if (t.tpe.typeSymbol.fullName.startsWith("scala.Function")) =>
        val bodyType = c.WeakTypeTag(t.tpe.typeArgs.takeRight(1).head)
        val params = (1 until t.tpe.typeArgs.size).map(i => Ident(s"x$i")).toList
        run(FunctionApply(ast, params), params.zip(paramsTypes[T]))(r, s, bodyType)

      case ast =>
        run[R, S, T](ast, Nil)
    }
  }

  private def run[R, S, T](ast: Ast, params: List[(Ident, Type)])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    ast match {
      case ast if ((t.tpe <:< c.weakTypeTag[Action[Any]].tpe)) =>
        runAction[S](ast, params)

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
      c.fail(s"Can't unquote $quoted")
    }

  private def paramsTypes[T](implicit t: WeakTypeTag[T]) =
    t.tpe.typeArgs.dropRight(1)

}
