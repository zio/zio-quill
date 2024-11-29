package io.getquill.util

import io.getquill.idiom.Idiom
import io.getquill.util.IndentUtil._
import io.getquill.util.Messages.{debugEnabled, errorPrefix, prettyPrint}
import io.getquill.quat.VerifyNoBranches

import scala.reflect.api.Position
import scala.reflect.macros.blackbox.{Context => MacroContext}

object MacroContextExt {

  private[getquill] val queryLogger: QueryLogger = new QueryLogger(Messages.quillLogFile)

  implicit final class RichContext(private val c: MacroContext) extends AnyVal {

    def error(msg: String): Unit =
      c.error(c.enclosingPosition, if (errorPrefix) s"[quill] $msg" else msg)

    def failAtPoint(msg: String, point: Int): Nothing = {
      val errorPos = c.enclosingPosition.withPoint(point)
      c.abort(errorPos, if (errorPrefix) s"[quill] $msg" else msg)
    }

    def fail(msg: String): Nothing =
      c.abort(c.enclosingPosition, if (errorPrefix) s"[quill] $msg" else msg)

    def warn(msg: String): Unit =
      c.warning(c.enclosingPosition, msg)

    def warn(verifyOutput: VerifyNoBranches.Output): Unit = {
      val pos            = c.enclosingPosition
      val locationString = s"${pos.source.path}:${pos.line}:${pos.column}"
      verifyOutput.messages.distinct.foreach(message =>
        println(s"[WARNING] ${locationString} Questionable row-class found.\n${message.msg}")
      )
    }

    def query(queryString: String, idiom: Idiom): Unit = {
      val formatted =
        if (prettyPrint) idiom.format(queryString) else queryString
      val output =
        if (formatted.fitsOnOneLine)
          formatted
        else
          "\n" + formatted.multiline(1, "| ") + "\n\n"

      queryLogger(output, c.enclosingPosition.source.path, c.enclosingPosition.line, c.enclosingPosition.column)

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
