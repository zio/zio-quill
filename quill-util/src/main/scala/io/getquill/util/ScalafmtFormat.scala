package io.getquill.util

import io.getquill.util.ThrowableOps._
import org.scalafmt.config.ScalafmtConfig
import org.scalafmt.{ Formatted, Scalafmt }

/**
 * Based on ScalaFmt210 from scalafmt cli
 */
object ScalafmtFormat {
  def apply(code: String, showErrorTrace: Boolean = false): String = {
    val style = ScalafmtConfig.default
    Scalafmt.format(code, style, Set.empty, "<input>") match {
      case Formatted.Success(formattedCode) =>
        formattedCode
      case Formatted.Failure(e) =>
        if (showErrorTrace)
          println(
            s"""===== Failed to format the code ====
               |$code
               |---
               |${e.stackTraceToString}.
               |""".stripMargin
          )
        code
    }
  }
}
