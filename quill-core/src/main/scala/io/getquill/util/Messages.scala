package io.getquill.util

import scala.reflect.macros.blackbox.{ Context => MacroContext }

object Messages {

  private val debugEnabled = {
    !sys.env.get("quill.macro.log").filterNot(_.isEmpty).map(_.toLowerCase).contains("false") &&
      !Option(System.getProperty("quill.macro.log")).filterNot(_.isEmpty).map(_.toLowerCase).contains("false")
  }

  private val traceEnabled = false
  private val traceColors = false

  def fail(msg: String) =
    throw new IllegalStateException(msg)

  def trace[T](label: String) =
    (v: T) =>
      {
        if (traceEnabled)
          println(s"$label${{ if (traceColors) pprint.apply(v, height = Integer.MAX_VALUE).render else pprint.apply(v, height = Integer.MAX_VALUE).plainText }.split("\n").map("    " + _).mkString("\n")}")
        v
      }

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
