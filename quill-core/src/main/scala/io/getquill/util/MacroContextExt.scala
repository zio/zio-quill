package io.getquill.util

import io.getquill.idiom.Idiom
import io.getquill.util.IndentUtil._
import io.getquill.util.Messages.{ LogToFile, debugEnabled, prettyPrint, quillLogFile }
import zio._
import zio.logging._

import java.nio.file.Paths
import java.time.ZonedDateTime
import scala.reflect.macros.blackbox.{ Context => MacroContext }

object MacroContextExt {

  val env = {
    quillLogFile match {
      case LogToFile.Enabled(file) =>
        ZEnv.live >>>
          Logging.file(
            logLevel = LogLevel.Info,
            format = LogFormat.fromFunction((ctx, str) => {
              str
            }),
            destination = Paths.get(file)
          ) >>> Logging.withRootLoggerName("query-logger")
    }
  }

  val runtime = Runtime.unsafeFromLayer(env)

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

      quillLogFile match {
        case LogToFile.Enabled(_) =>
          runtime.unsafeRunAsync_(log.info(
            s"""
                 |-- file: ${c.enclosingPosition.source.path}
                 |-- line: ${c.enclosingPosition.line}
                 |-- col: ${c.enclosingPosition.column}
                 |-- time: ${ZonedDateTime.now().format(LogDatetimeFormatter.isoLocalDateTimeFormatter)}
                 |${idiom.format(queryString)};
                 |""".stripMargin
          ))
      }

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
