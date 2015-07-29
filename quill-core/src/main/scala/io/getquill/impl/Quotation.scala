package io.getquill.impl

import scala.reflect.macros.whitebox.Context
import io.getquill.util.Messages

trait Quoted[T]

class Quotation(val c: Context) extends Messages {

  import c.universe._

  case class QuotedTree(tree: Any)

  def unquote[T](quoted: c.Expr[Quoted[T]]) =
    extract(quoted.tree).getOrElse {
      fail(s"Can't find the original quoted tree at ${quoted.tree}.")
    }

  def quote[T: WeakTypeTag](body: c.Expr[T]) =
    q"""
      new ${c.weakTypeOf[Quoted[T]]} {
        @${c.weakTypeOf[QuotedTree]}(${body.tree})
        def tree = ()
        override def toString = ${body.tree.toString}
      }
    """

  private def extract(quoted: Tree) =
    for {
      method <- quoted.tpe.decls.find(_.name.decodedName.toString == "tree")
      annotation <- method.annotations.headOption
      tree <- annotation.tree.children.lastOption
    } yield {
      tree
    }
}
