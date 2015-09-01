package io.getquill.source

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Action
import io.getquill.ast.Ast
import io.getquill.ast.Function
import io.getquill.ast.Query
import io.getquill.norm.Normalize
import io.getquill.quotation.Quotation
import io.getquill.quotation.Quoted
import io.getquill.util.Messages.RichContext

trait SourceMacro extends Quotation with ActionMacro with QueryMacro {
  val c: Context
  import c.universe.{ Function => _, _ }

  protected def toExecutionTree(ast: Ast): Tree

  def run[R, S, T](quoted: Expr[Quoted[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    Normalize(ast(quoted)) match {

      case Function(params, action: Action) =>
        runAction[S](action, params.zip(paramsTypes[T]))
      case action: Action =>
        runAction(action)

      case Function(params, query: Query) =>
        val tr = c.WeakTypeTag(t.tpe.typeArgs.takeRight(1).head.typeArgs.head)
        runQuery(query, params.zip(paramsTypes[T]))(r, s, tr)
      case query: Query =>
        val tr = c.WeakTypeTag(t.tpe.typeArgs.takeRight(1).head)
        runQuery(query, List())(r, s, tr)

      case other =>
        c.fail(s"Not runnable $other")
    }

  private def ast[T](quoted: Expr[Quoted[T]]) =
    unquote[Ast](quoted.tree).getOrElse {
      c.fail("Can't unquote $quoted")
    }

  private def paramsTypes[T](implicit t: WeakTypeTag[T]) =
    t.tpe.typeArgs.dropRight(1)

}
