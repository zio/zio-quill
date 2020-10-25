package io.getquill

import scala.language.experimental.macros

/**
 * Convenience API that allows construction of a Quat using `Quat.from[T]`
 */
package object quat {

  def quatOf[T]: Quat = macro QuatMacro.makeQuat[T]
}
