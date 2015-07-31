package io.getquill.impl

import scala.reflect.macros.whitebox.Context
import io.getquill.util.Messages
import scala.annotation.StaticAnnotation

trait Quoted[T]

class Quotation(val c: Context) extends Messages {

  import c.universe._

  case class QuotedTree(tree: Any) extends StaticAnnotation

  def quote[T: WeakTypeTag](body: c.Expr[T]) =
    q"""
      new ${c.weakTypeOf[Quoted[T]]} {
        @${c.weakTypeOf[QuotedTree]}(${body.tree})
        def tree = ()
        override def toString = ${body.tree.toString}
      }
    """

  def unquote[T](quoted: c.Expr[Quoted[T]]) = {
    val method =
      quoted.tree.tpe.decls.find(_.name.decodedName.toString == "tree")
        .getOrElse(fail(s"Can't find the tree method at ${quoted.tree}: ${quoted.tree.tpe}"))
    val annotation =
      method.annotations.headOption
        .getOrElse(fail(s"Can't find the QuotedTree annotation at $method"))
    annotation.tree.children.lastOption
      .getOrElse(fail(s"Can't find the QuotedTree body at $annotation"))
  }
}
