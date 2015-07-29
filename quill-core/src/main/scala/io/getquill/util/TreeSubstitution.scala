package io.getquill.util

import scala.reflect.macros.whitebox.Context

trait TreeSubstitution {

  val c: Context
  import c.universe._

  def substituteTree(tree: Tree, from: List[Tree], to: List[Tree]) = {
    val cast = tree.asInstanceOf[scala.reflect.internal.Trees#Apply]
    val trees = classOf[scala.reflect.internal.Trees#Apply].getField("$outer").get(cast).asInstanceOf[scala.reflect.internal.Trees]
    val subs = new trees.TreeSubstituter(unsafeCast(from.map(_.symbol)), unsafeCast(to))
    subs.transform(unsafeCast(tree)).asInstanceOf[Tree]
  }

  private def unsafeCast[T](value: Any) = value.asInstanceOf[T]
}