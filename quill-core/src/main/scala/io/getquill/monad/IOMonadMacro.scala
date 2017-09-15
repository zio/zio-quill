package io.getquill.monad

import scala.reflect.macros.blackbox.{ Context => MacroContext }

class IOMonadMacro(val c: MacroContext) {
  import c.universe._

  def runIO(quoted: Tree): Tree =
    q"${c.prefix}.Run(() => ${c.prefix}.run($quoted))"
}
