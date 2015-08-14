package io.getquill.util

import language.implicitConversions
import scala.reflect.macros.whitebox.Context

// Keep calm, I know that this is very bad but I couldn't figure out an alternative.
// Here goes a meme so you won't be mad at me if you have to maintain this code:
// http://cdn.meme.am/instances/57874036.jpg
object SubstituteTrees {

  def apply(c: Context)(tree: c.Tree, from: List[c.Tree], to: List[c.Tree]): c.Tree = {
    import c.universe._

    implicit def unsafeCast[T](value: Any) = value.asInstanceOf[T]

    val trees: scala.reflect.internal.Trees = tree.getClass.getField("$outer").get(tree)

    val substituter = new trees.TreeSubstituter(from.map(_.symbol), to)

    substituter.transform(unsafeCast(tree))
  }
}