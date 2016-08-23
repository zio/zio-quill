package io.getquill.dsl.macroz

import scala.reflect.macros.whitebox.{ Context => MacroContext }

private[dsl] class DslMacro(val c: MacroContext)
  extends LiftingMacro
  with ExpandActionMacro