package io.getquill.monad

import scala.reflect.macros.blackbox.{ Context => MacroContext }
import scala.concurrent.ExecutionContext

class IOMonadMacro(val c: MacroContext) {
  import c.universe._

  def runIO(quoted: Tree): Tree =
    q"${c.prefix}.Run(() => ${c.prefix}.run($quoted))"

  def runIOEC(quoted: Tree): Tree = {
    // make sure we're shadowing the current ec implicit
    val ecName =
      c.inferImplicitValue(c.weakTypeOf[ExecutionContext]) match {
        case Select(_, name) =>
          name.decodedName.toString
        case tree =>
          tree.symbol.name.decodedName.toString
      }

    val v = q"implicit val ${TermName(ecName)}: scala.concurrent.ExecutionContext"
    q"${c.prefix}.Run($v => ${c.prefix}.run($quoted))"
  }
}
