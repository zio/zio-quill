package io.getquill.quotation

import scala.annotation.StaticAnnotation
import scala.reflect.ClassTag
import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Ast
import io.getquill.util.Messages.RichContext

trait Quoted[+T] {
  def ast: Ast
}

case class QuotedAst(ast: Ast) extends StaticAnnotation

trait Quotation extends Parsing with Liftables with Unliftables {

  val c: Context
  import c.universe._

  def quote[T: WeakTypeTag](body: Expr[T]) = {
    val ast = astParser(body.tree)
    verifyFreeVariables(ast)
    q"""
      new ${c.weakTypeOf[Quoted[T]]} {
        @${c.weakTypeOf[QuotedAst]}($ast)
        def quoted = ()
        override def ast = $ast
        override def toString = ast.toString
      }
    """
  }

  protected def unquote[T](tree: Tree)(implicit ct: ClassTag[T]) =
    astTree(tree).flatMap(astUnliftable.unapply).map {
      case ast: T => ast
      case other  => c.fail(s"Expected a '${ct.runtimeClass.getSimpleName}', but got '$other'")
    }

  private def astTree(tree: Tree) =
    for {
      method <- tree.tpe.decls.find(_.name.decodedName.toString == "quoted")
      annotation <- method.annotations.headOption
      astTree <- annotation.tree.children.lastOption
    } yield (astTree)

  private def verifyFreeVariables(ast: Ast) =
    FreeVariables(ast).toList match {
      case Nil  =>
      case vars => c.fail(s"A quotation must not have references to variables outside it's scope. Found: '${vars.mkString(", ")}' in '$ast'.")
    }
}
