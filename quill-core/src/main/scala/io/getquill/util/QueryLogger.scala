package io.getquill.util

import io.getquill.util.Messages.LogToFile
import zio._
import zio.logging._

import java.nio.file.Paths
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class QueryLogger(logToFile: LogToFile) {

  val runtime: Option[Runtime.Scoped[Unit]] =
    logToFile match {
      case LogToFile.Enabled(logFile) =>
        Unsafe.unsafe { implicit u =>
          Some(
            Runtime.unsafe.fromLayer(
              fileLogger(
                FileLoggerConfig(
                  destination = Paths.get(logFile),
                  format = LogFormat.line,
                  filter = LogFilter.logLevel(LogLevel.Info)
                )
              )
            )
          )
        }
      case LogToFile.Disabled => None
    }

  def apply(queryString: String, sourcePath: String, line: Int, column: Int): Unit =
    runtime match {
      case Some(runtimeValue) =>
        Unsafe.unsafe { implicit u =>
          runtimeValue.unsafe
            .run(
              ZIO.logInfo(
                s"""
                   |-- file: $sourcePath:$line:$column
                   |-- time: ${ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}
                   |$queryString;
                   |""".stripMargin
              )
            )
            .getOrThrow()
        }
      case None => // do nothing
    }
}
