package io.getquill.quotation

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Ast
import io.getquill.util.Messages.RichContext

trait Quoted[+T]

trait Quotation extends Parsing with Liftables with Unliftables {

  val c: Context
  import c.universe._

  case class QuotedAst(ast: Ast) extends StaticAnnotation

  def quote[T: WeakTypeTag](body: Expr[T]) = {
    val ast = astParser(body.tree)
    verifyFreeVariables(ast)
    q"""
      new ${c.weakTypeOf[Quoted[T]]} {
        @${c.weakTypeOf[QuotedAst]}($ast)
        def ast: io.getquill.ast.Ast = $ast
        override def toString = ast.toString
      }
    """
  }

  protected def unquote[T](tree: Tree)(implicit ct: ClassTag[T]) = {
    val method =
      tree.tpe.decls.find(_.name.decodedName.toString == "ast")
        .getOrElse(c.fail(s"Can't find the ast method at ${tree}: ${tree.tpe}"))
    val annotation =
      method.annotations.headOption
        .getOrElse(c.fail(s"Can't find the QuotedAst annotation at $method"))
    val astTree =
      annotation.tree.children.lastOption
        .getOrElse(c.fail(s"Can't find the QuotedAst body at $annotation"))
    astUnliftable.unapply(astTree) map {
      case ast: T => ast
      case other  => c.fail(s"Expected a '${ct.runtimeClass.getSimpleName}', but got '$other'")
    }
  }

  private def verifyFreeVariables(ast: Ast) =
    FreeVariables(ast).toList match {
      case Nil  =>
      case vars => c.fail(s"A quotation must not have references to variables outside it's scope. Found: '${vars.mkString(", ")}' in '$ast'.")
    }
}
