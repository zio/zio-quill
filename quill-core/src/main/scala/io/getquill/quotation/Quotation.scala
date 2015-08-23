package io.getquill.quotation

import scala.annotation.StaticAnnotation
import scala.reflect.macros.whitebox.Context
import io.getquill.util.Messages._

trait Quoted[+T]

trait Quotation extends Unliftables with Liftables {

  val c: Context
  import c.universe._

  case class QuotedTree(tree: Any) extends StaticAnnotation

  def quote[T: WeakTypeTag](body: Expr[T]) = {
    verifyFreeVariables(body.tree)
    val ast = astUnliftable(body.tree)
    q"""
      new ${c.weakTypeOf[Quoted[T]]} {
        @${c.weakTypeOf[QuotedTree]}(${body.tree})
        def ast = $ast
        override def toString = ast.toString
      }
    """
  }

  protected def unquote[T](tree: Tree) = {
    val method =
      tree.tpe.decls.find(_.name.decodedName.toString == "ast")
        .getOrElse(c.fail(s"Can't find the tree method at ${tree}: ${tree.tpe}"))
    val annotation =
      method.annotations.headOption
        .getOrElse(c.fail(s"Can't find the QuotedTree annotation at $method"))
    annotation.tree.children.lastOption
      .getOrElse(c.fail(s"Can't find the QuotedTree body at $annotation"))
  }

  private def verifyFreeVariables(tree: Tree) =
    freeVariables(tree) match {
      case Nil  =>
      case vars => c.fail(s"A quotation must not have references to free variables. Found: ${vars.mkString(", ")}")
    }

  private def freeVariables(tree: Tree, known: List[Symbol] = List()): List[String] =
    tree match {
      case t if (t.tpe <:< c.weakTypeTag[Quoted[Any]].tpe) =>
        List()
      case Select(This(_), TermName(name)) if (name != "Predef") =>
        List(name)
      case i: Ident if (isVariable(i.symbol) && i.toString != "_" && !known.contains(i.symbol)) =>
        List(i.toString)
      case q"(..$params) => $body" =>
        freeVariables(body, known ++ params.map(_.symbol))
      case q"new { def apply[..$t1](...$params) = $body }" =>
        freeVariables(body, known ++ params.flatten.map(_.symbol))
      case q"$tuple match { case (..$params) => $body }" =>
        freeVariables(body, known ++ params.map(_.symbol))
      case tree if (tree.children.nonEmpty) =>
        tree.children.map(freeVariables(_, known)).flatten
      case other =>
        List()
    }

  private def isVariable(s: Symbol) =
    !s.isPackage && !s.isMethod && !s.isModule && !s.isClass && !s.isType
}
