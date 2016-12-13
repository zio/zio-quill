package io.getquill.util

import scala.reflect.macros.blackbox.{ Context => MacroContext }

object Messages {

  private val debugEnabled = {
    !sys.env.get("quill.macro.log").filterNot(_.isEmpty).map(_.toLowerCase).contains("false") &&
      !Option(System.getProperty("quill.macro.log")).filterNot(_.isEmpty).map(_.toLowerCase).contains("false")
  }

  def fail(msg: String) =
    throw new IllegalStateException(msg)

  implicit class RichContext(c: MacroContext) {

    def error(msg: String): Unit =
      c.error(c.enclosingPosition, msg)

    def fail(msg: String): Nothing =
      c.abort(c.enclosingPosition, msg)

    def warn(msg: String): Unit =
      c.warning(c.enclosingPosition, msg)

    def info(msg: String): Unit =
      if (debugEnabled) c.info(c.enclosingPosition, msg, force = true)

    def debug[T](v: T): T = {
      info(v.toString)
      v
    }
  }
}
