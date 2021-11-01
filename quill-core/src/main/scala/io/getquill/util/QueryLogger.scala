package io.getquill.util

import io.getquill.idiom.Idiom
import io.getquill.util.Messages.{ LogToFile, quillLogFile }
import zio._
import zio.logging._

import java.nio.file.Paths
import java.time.ZonedDateTime

class QueryLogger(logToFile: LogToFile) {
  private[getquill] val env = {
    logToFile match {
      case LogToFile.Enabled(file) =>
        ZEnv.live >>>
          Logging.file(
            logLevel = LogLevel.Info,
            format = LogFormat.fromFunction((ctx, str) => {
              str
            }),
            destination = Paths.get(file)
          ) >>> Logging.withRootLoggerName("query-logger")
      case LogToFile.Disabled => Logging.ignore
    }
  }

  private[getquill] val runtime = Runtime.unsafeFromLayer(env)

  def apply(queryString: String, sourcePath: String, line: Int, column: Int): Unit = {
    quillLogFile match {
      case LogToFile.Enabled(_) =>
        runtime.unsafeRunAsync_(log.info(
          s"""
             |-- file: $sourcePath
             |-- line: $line
             |-- col: $column
             |-- time: ${ZonedDateTime.now().format(LogDatetimeFormatter.isoLocalDateTimeFormatter)}
             |$queryString;
             |""".stripMargin
        ))
      case LogToFile.Disabled => // do nothing
    }
  }
}
