package io.getquill.quat

import io.getquill.quotation.LiftUnlift

import scala.reflect.macros.whitebox.{ Context => MacroContext }

class QuatMacro(val c: MacroContext) extends QuatMaking {
  import c.universe._

  def makeQuat[T: c.WeakTypeTag]: c.Tree = {
    val quat = inferQuat(implicitly[c.WeakTypeTag[T]].tpe)
    val liftUnlift = new { override val mctx: c.type = c } with LiftUnlift(quat.countFields)
    val quatExpr: c.Tree = liftUnlift.quatLiftable(quat)
    q"${quatExpr}"
  }
}
