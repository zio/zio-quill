package io.getquill.util

import io.getquill.util.Messages.{ debugEnabled, prettyPrint }
import io.getquill.idiom.Idiom
import io.getquill.util.IndentUtil._

import scala.reflect.macros.blackbox.{ Context => MacroContext }

object MacroContextExt {

  implicit class RichContext(c: MacroContext) {

    def error(msg: String): Unit =
      c.error(c.enclosingPosition, msg)

    def fail(msg: String): Nothing =
      c.abort(c.enclosingPosition, msg)

    def warn(msg: String): Unit =
      c.warning(c.enclosingPosition, msg)

    def query(queryString: String, idiom: Idiom): Unit = {
      val formatted =
        if (prettyPrint) idiom.format(queryString) else queryString
      val output =
        if (formatted.fitsOnOneLine)
          formatted
        else
          "\n" + formatted.multiline(1, "| ") + "\n\n"

      if (debugEnabled) c.info(c.enclosingPosition, output, force = true)
    }

    def info(msg: String): Unit =
      if (debugEnabled) c.info(c.enclosingPosition, msg, force = true)

    def debug[T](v: T): T = {
      info(v.toString)
      v
    }
  }
}
