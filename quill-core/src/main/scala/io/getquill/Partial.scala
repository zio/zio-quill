package io.getquill

import scala.language.dynamics
import language.experimental.macros
import io.getquill.ast.Parametrized
import io.getquill.ast.ParametrizedShow.parametrizedShow
import io.getquill.attach.Attachable
import util.Show.Shower

trait Partial[P, +T] extends Attachable[Parametrized] with Dynamic {

  def applyDynamic(selection: String)(args: Any*): Any = macro PartialMacroApply.apply[T]
  def applyDynamicNamed(selection: String)(args: (String, Any)*): Any = macro PartialMacroApply.applyNamed[T]

  override def toString = attachment.show
}

object Partial {

  def apply[P1, T](f: P1 => T): Any = macro PartialMacroCreate.create1[P1, T]
  def apply[P1, P2, T](f: (P1, P2) => T): Any = macro PartialMacroCreate.create2[P1, P2, T]
}
