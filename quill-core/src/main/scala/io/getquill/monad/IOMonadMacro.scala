package io.getquill.monad

import io.getquill.util.Messages._
import scala.reflect.macros.blackbox.{ Context => MacroContext }

class IOMonadMacro(val c: MacroContext) {
  import c.universe._

  def runIO(quoted: Tree): Tree =
    c.debug(q"${c.prefix}.Run(() => ${c.prefix}.run($quoted))")
}
