package io.getquill.context

import scala.reflect.macros.whitebox.{ Context => MacroContext }

import io.getquill.ast.Ast
import io.getquill.ast.Dynamic
import io.getquill.ast.FunctionApply
import io.getquill.ast.Ident
import io.getquill.dsl.CoreDsl
import io.getquill.quotation.Bindings
import io.getquill.quotation.FreeVariables
import io.getquill.quotation.Quotation
import io.getquill.util.Messages.RichContext

trait ContextMacro extends Quotation with ActionMacro with QueryMacro with QueryProbingMacro {
  val c: MacroContext
  import c.universe.{ Function => _, Ident => _, _ }

  protected def prepare(ast: Ast, params: List[Ident]): Tree

  def run[R, S](quoted: Expr[_])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S]): Tree = runExpr(quoted, true)(r, s)

  def runSingle[R, S](quoted: Expr[_])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S]): Tree = runExpr(quoted, false)(r, s)

  private def runExpr[R, S](quoted: Expr[_], returnList: Boolean)(implicit r: WeakTypeTag[R], s: WeakTypeTag[S]): Tree = {
    implicit val t = c.WeakTypeTag(quoted.actualType.baseType(c.weakTypeOf[CoreDsl#Quoted[Any]].typeSymbol).typeArgs.head)

    val ast = this.ast(quoted)

    FreeVariables(ast) match {
      case free if free.isEmpty =>
      case free =>
        c.fail(s"""
          |Found the following free variables: ${free.mkString(", ")}.
          |Quotations can't reference values outside their scope directly. 
          |In order to bind runtime values to a quotation, please use the method `lift`.
          |Example: `def byName(n: String) = quote(query[Person].filter(_.name == lift(n)))`
        """.stripMargin)
    }

    val inPlaceParams = bindingsTree(q"quoted", quoted.tree.tpe)

    t.tpe.typeSymbol.fullName.startsWith("scala.Function") match {

      case true =>
        val bodyType = c.WeakTypeTag(t.tpe.typeArgs.takeRight(1).head)
        val params = (1 until t.tpe.typeArgs.size).map(i => Ident(s"p$i")).toList
        run(quoted.tree, FunctionApply(ast, params), inPlaceParams, params.zip(paramsTypes(t)), returnList)(r, s, bodyType)

      case false =>
        run(quoted.tree, ast, inPlaceParams, Nil, returnList)(r, s, t)
    }
  }

  private def bindingsTree(tree: Tree, tpe: Type) = {
    Bindings(c)(tree, tpe)
      .map {
        case (symbol, tree) =>
          (Ident(symbol.name.decodedName.toString), (symbol.typeSignature.resultType, tree))
      }.toMap
  }

  private def run[R, S, T](quotedTree: Tree, ast: Ast, inPlaceParams: collection.Map[Ident, (Type, Tree)], params: List[(Ident, Type)], returnList: Boolean)(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    ast match {
      case ast if ((t.tpe.erasure <:< c.weakTypeTag[CoreDsl#Action[Any, Any]].tpe.erasure)) =>
        runAction[R, S, T](quotedTree, ast, inPlaceParams, params)

      case ast =>
        runQuery(quotedTree, ast, inPlaceParams, params, returnList)(r, s, queryType(t.tpe))
    }

  private def queryType(tpe: Type) =
    if (tpe <:< c.typeOf[CoreDsl#Query[_]])
      c.WeakTypeTag(tpe.baseType(c.typeOf[CoreDsl#Query[_]].typeSymbol).typeArgs.head)
    else
      c.WeakTypeTag(tpe)

  private def ast[T](quoted: Expr[_]) =
    unquote[Ast](quoted.tree).getOrElse {
      Dynamic(quoted.tree)
    }

  private def paramsTypes[T](implicit t: WeakTypeTag[T]) =
    t.tpe.typeArgs.dropRight(1)

}
