package io.getquill.sources

import scala.reflect.macros.whitebox.Context
import io.getquill._
import io.getquill.ast.{ Action => _, Query => _, _ }
import io.getquill.quotation.Quotation
import io.getquill.quotation.Quoted

trait SourceMacro extends Quotation with ActionMacro with QueryMacro with ResolveSourceMacro {
  val c: Context
  import c.universe.{ Function => _, Ident => _, _ }

  protected def prepare(ast: Ast, params: List[Ident]): Tree

  def run[R, S](quoted: Expr[Quoted[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S]): Tree = {
    implicit val t = c.WeakTypeTag(quoted.actualType.baseType(c.weakTypeOf[Quoted[Any]].typeSymbol).typeArgs.head)

    val ast = this.ast(quoted)

    val inPlaceParams = bindingsTree(quoted.tree)

    t.tpe.typeSymbol.fullName.startsWith("scala.Function") match {

      case true =>
        val bodyType = c.WeakTypeTag(t.tpe.typeArgs.takeRight(1).head)
        val params = (1 until t.tpe.typeArgs.size).map(i => Ident(s"p$i")).toList
        run(quoted.tree, FunctionApply(ast, params), inPlaceParams, params.zip(paramsTypes(t)))(r, s, bodyType)

      case false =>
        run(quoted.tree, ast, inPlaceParams, Nil)(r, s, t)
    }
  }

  private def bindingsTree(tree: Tree) =
    tree.tpe.decls
      .filter(_.name.decodedName.toString.startsWith("binding_"))
      .map { symbol =>
        (Ident(symbol.name.decodedName.toString.replace("binding_", "")), (symbol.typeSignature.resultType, q"quoted.$symbol"))
      }.toMap

  private def run[R, S, T](quotedTree: Tree, ast: Ast, inPlaceParams: collection.Map[Ident, (Type, Tree)], params: List[(Ident, Type)])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    ast match {
      case ast if ((t.tpe.erasure <:< c.weakTypeTag[Action[Any]].tpe.erasure)) =>
        runAction[S, T](quotedTree, ast, inPlaceParams, params)

      case ast =>
        runQuery(quotedTree, ast, inPlaceParams, params)(r, s, queryType(t.tpe))
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

}
