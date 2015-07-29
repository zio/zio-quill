package io.getquill.util

import scala.reflect.macros.whitebox.Context

// Keep calm, I know that this is very bad but I couldn't figure out an alternative.
// Here goes a meme so you won't be mad at me if you have to maintain this code:
// http://cdn.meme.am/instances/57874036.jpg
trait TreeSubstitution {

  val c: Context
  import c.universe._

  def substituteTree(tree: Tree, from: List[Tree], to: List[Tree]) =
    substituter(tree, from, to).transform(unsafeCast(tree)).asInstanceOf[Tree]

  private def substituter(tree: Tree, from: List[Tree], to: List[Tree]) = {
    val trees = this.trees(tree)
    new trees.TreeSubstituter(unsafeCast(from.map(_.symbol)), unsafeCast(to))
  }

  private def trees(tree: Tree) =
    tree.getClass.getField("$outer").get(tree)
      .asInstanceOf[scala.reflect.internal.Trees]

  private def unsafeCast[T](value: Any) = value.asInstanceOf[T]
}