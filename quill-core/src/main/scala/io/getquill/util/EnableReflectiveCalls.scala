package io.getquill.util

import scala.reflect.macros.blackbox

object EnableReflectiveCalls {

  def apply(c: blackbox.Context): List[c.universe.Tree] = {
    import c.universe._
    List(
      q"import _root_.scala.language.reflectiveCalls",
      q"Nil.asInstanceOf[{ def size }].size"
    )
  }
}
