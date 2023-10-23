package io.getquill.util

import io.getquill.util.Messages.LogToFile
import zio._
import zio.logging._
import zio.logging.extensions.executeWithLogger

import java.nio.file.Paths
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class QueryLogger(logToFile: LogToFile) {

  def apply(queryString: String, sourcePath: String, line: Int, column: Int): Unit =
    logToFile match {
      case LogToFile.Enabled(logFile) =>
        val config =
          FileLoggerConfig(
            destination = Paths.get(logFile),
            format = LogFormat.line,
            filter = LogFilter.logLevel(LogLevel.Info)
          )

        executeWithLogger(config) {
          ZIO
            .logInfo(
              s"""
                 |-- file: $sourcePath:$line:$column
                 |-- time: ${ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}
                 |$queryString;
                 |""".stripMargin
            )
        }
      case LogToFile.Disabled => // do nothing
    }
}
