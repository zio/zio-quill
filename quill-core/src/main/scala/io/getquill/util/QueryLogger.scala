package io.getquill.util

import io.getquill.util.Messages.LogToFile
import zio._
import zio.logging._

import java.nio.file.Paths
import java.time.ZonedDateTime

class QueryLogger(logToFile: LogToFile) {

  def produceLoggerEnv(file: String) =
    ZEnv.live >>>
      Logging.file(
        logLevel = LogLevel.Info,
        format = LogFormat.fromFunction((ctx, str) => {
          str
        }),
        destination = Paths.get(file)
      ) >>> Logging.withRootLoggerName("query-logger")

  lazy val env = {
    logToFile match {
      case LogToFile.Enabled(file) => Some(produceLoggerEnv(file))
      case LogToFile.Disabled      => None
    }
  }

  private[getquill] val runtime =
    env.map(Runtime.unsafeFromLayer(_))

  def apply(
      queryString: String,
      sourcePath: String,
      line: Int,
      column: Int
  ): Unit = {
    runtime match {
      case Some(runtimeValue) =>
        runtimeValue.unsafeRunAsync_(
          log.info(
            s"""
             |-- file: $sourcePath:$line:$column
             |-- time: ${ZonedDateTime
              .now()
              .format(LogDatetimeFormatter.isoLocalDateTimeFormatter)}
             |$queryString;
             |""".stripMargin
          )
        )
      case None => // do nothing
    }
  }
}
