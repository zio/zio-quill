package io.getquill.meta

import language.experimental.macros
import scala.reflect.macros.whitebox.Context

class MetaMacro(val c: Context) {

  import c.universe._

  case class MetaTree(tree: Any)

  def unquote[T](meta: c.Expr[Meta[T]]) =
    detach(meta.tree)

  def quote[T](body: c.Expr[T])(implicit t: WeakTypeTag[T]) = {
    require(internal.freeTerms(body.tree).isEmpty)
    q"""
      new io.getquill.meta.Meta[$t] {
        @${c.weakTypeOf[MetaTree]}(${body.tree})
        def tree = ()
        override def toString = ${body.tree.toString}
      }
    """
  }

  private def detach(tree: Tree) = {
    val method =
      tree.tpe.decls.find(_.name.decodedName.toString == "tree")
        .getOrElse(c.abort(c.enclosingPosition, s"Can't find the tree method at '${tree.tpe}'. $tree"))
    val annotation =
      method.annotations.headOption
        .getOrElse(c.abort(c.enclosingPosition, s"Can't find the MetaTree annotation at '$method'. $tree"))
    annotation.tree.children.last
  }
}
