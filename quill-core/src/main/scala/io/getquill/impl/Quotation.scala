package io.getquill.impl

import scala.annotation.StaticAnnotation
import scala.reflect.macros.whitebox.Context

import io.getquill.util.Messages._

trait Quoted[+T]

trait Quotation {

  val c: Context
  import c.universe._

  case class QuotedTree(tree: Any) extends StaticAnnotation

  def quote[T: WeakTypeTag](body: c.Expr[T]) = {
    verifyFreeVariables(body.tree)
    q"""
      new ${c.weakTypeOf[Quoted[T]]} {
        @${c.weakTypeOf[QuotedTree]}(${body.tree})
        def tree = ()
        override def toString = ${body.tree.toString}
      }
    """
  }

  def unquote[T](quoted: c.Expr[Quoted[T]]) =
    unquoteTree(quoted.tree)

  protected def unquoteTree[T](tree: Tree) = {
    val method =
      tree.tpe.decls.find(_.name.decodedName.toString == "tree")
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
      case Select(This(TypeName(tpe)), TermName(name)) if (name != "Predef") =>
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
