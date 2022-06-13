package io.getquill.util

import scala.reflect.macros.whitebox.{ Context => MacroContext }

class PrintMacMacro(val c: MacroContext) {
  import c.universe._

  def apply(value: Tree): Tree = {
    println(
      "================= Printing Tree =================\n" +
        show(value)
    )
    q"()"
  }

}
