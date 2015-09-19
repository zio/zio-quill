package io.getquill.source

import scala.reflect.macros.whitebox.Context

import io.getquill._
import io.getquill.ast.{ Query => QueryAst, Action => ActionAst, _ }
import io.getquill.norm.Normalize
import io.getquill.quotation.Quotation
import io.getquill.quotation.Quoted
import io.getquill.util.Messages.RichContext

trait SourceMacro extends Quotation with ActionMacro with QueryMacro with ResolveSourceMacro {
  val c: Context
  import c.universe.{ Function => _, Ident => _, _ }

  protected def toExecutionTree(ast: Ast): Tree

  def run[R, S, T](quoted: Expr[Quoted[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {

    Normalize(ast(quoted)) match {

      case Function(params, ast) =>
        val bodyType = c.WeakTypeTag(t.tpe.typeArgs.takeRight(1).head)
        run(ast, params.zip(paramsTypes[T]))(r, s, bodyType)

      case ast =>
        run[R, S, T](ast, List())
    }
  }

  private def run[R, S, T](ast: Ast, params: List[(Ident, Type)])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    ast match {
      case ast: ActionAst =>
        runAction[S](ast, params)
      case ast: Infix if (t.tpe <:< c.weakTypeTag[Action[Any]].tpe) =>
        runAction[S](ast, params)

      case ast: QueryAst =>
        runQuery(ast, params)(r, s, queryType(t.tpe))
      case ast: Infix if (t.tpe <:< c.weakTypeTag[Query[Any]].tpe) =>
        runQuery(Map(ast, Ident("x"), Ident("x")), params)(r, s, queryType(t.tpe))

      case other =>
        c.fail(s"Not runnable $other")
    }

  private def queryType(tpe: Type) =
    c.WeakTypeTag(tpe.baseType(c.typeOf[Query[_]].typeSymbol).typeArgs.head)

  private def ast[T](quoted: Expr[Quoted[T]]) =
    unquote[Ast](quoted.tree).getOrElse {
      c.fail(s"Can't unquote $quoted")
    }

  private def paramsTypes[T](implicit t: WeakTypeTag[T]) =
    t.tpe.typeArgs.dropRight(1)

}
