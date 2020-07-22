package io.getquill.quat

import io.getquill.quotation.Liftables

import scala.reflect.macros.whitebox.{ Context => MacroContext }

class QuatMacro(val c: MacroContext) extends QuatMaking with Liftables {
  import c.universe._

  def makeQuat[T: c.WeakTypeTag]: c.Tree = q"${inferQuat(implicitly[c.WeakTypeTag[T]].tpe)}"
}
