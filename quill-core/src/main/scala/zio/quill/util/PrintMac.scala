package zio.quill.util

import scala.language.experimental.macros

object PrintMac {
  def apply(value: Any): Unit = macro PrintMacMacro.apply
}
