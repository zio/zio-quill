package zio.quill.util

import scala.language.experimental.macros
import scala.reflect.macros.blackbox.{Context => MacroContext}

object UntypecheckLiteral {
  def apply(value: Long): Any = macro UntypecheckLiteralMacro.untypecheck
}

class UntypecheckLiteralMacro(val c: MacroContext) {
  import c.universe._

  def untypecheck(value: Tree): Tree = q"(${c.untypecheck(value)}: Any)"
}
