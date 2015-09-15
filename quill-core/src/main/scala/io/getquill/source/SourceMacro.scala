package io.getquill.source

import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Action
import io.getquill.ast.Ast
import io.getquill.ast.Function
import io.getquill.ast.Query
import io.getquill.ast.Map
import io.getquill.ast.Ident
import io.getquill.norm.Normalize
import io.getquill.quotation.Quotation
import io.getquill.quotation.Quoted
import io.getquill.util.Messages.RichContext
import io.getquill.Queryable
import io.getquill.ast.Infix
import io.getquill.Actionable

trait SourceMacro extends Quotation with ActionMacro with QueryMacro with ResolveSourceMacro {
  val c: Context
  import c.universe.{ Function => _, Ident => _, _ }

  protected def toExecutionTree(ast: Ast): Tree

  def run[R, S, T](quoted: Expr[Quoted[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {

    object action {
      def unapply(ast: Ast) =
        ast match {
          case ast: Action =>
            Some(ast)
          case ast: Infix if (t.tpe <:< c.weakTypeTag[Actionable[Any]].tpe) =>
            Some(ast)
          case other =>
            None
        }
    }

    object query {
      def unapply(ast: Ast) =
        ast match {
          case ast: Query =>
            Some(ast)
          case ast: Infix if (t.tpe <:< c.weakTypeTag[Queryable[Any]].tpe) =>
            Some(Map(ast, Ident("x"), Ident("x")))
          case other =>
            None
        }
    }

    Normalize(ast(quoted)) match {

      case Function(params, action(ast)) =>
        runAction[S](ast, params.zip(paramsTypes[T]))
      case Function(params, query(ast)) =>
        val tr = c.WeakTypeTag(queryableType(t.tpe.typeArgs.takeRight(1).head))
        runQuery(ast, params.zip(paramsTypes[T]))(r, s, tr)

      case action(ast) =>
        runAction(ast)
      case query(ast) =>
        val tr = c.WeakTypeTag(queryableType(t.tpe))
        runQuery(ast, List())(r, s, tr)

      case other =>
        c.fail(s"Not runnable $other")
    }
  }

  private def queryableType(tpe: Type) =
    tpe.baseType(c.typeOf[Queryable[_]].typeSymbol).typeArgs.head

  private def ast[T](quoted: Expr[Quoted[T]]) =
    unquote[Ast](quoted.tree).getOrElse {
      c.fail(s"Can't unquote $quoted")
    }

  private def paramsTypes[T](implicit t: WeakTypeTag[T]) =
    t.tpe.typeArgs.dropRight(1)

}
