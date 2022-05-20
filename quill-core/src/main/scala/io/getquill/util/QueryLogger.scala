package io.getquill.util

import io.getquill.util.Messages.LogToFile
import zio._
import zio.logging._

import java.nio.file.Paths
import java.time.ZonedDateTime

class QueryLogger(logToFile: LogToFile) {

  private def produceLoggerEnv(file: String) =
    ZEnv.live >>>
      Logging.file(
        logLevel = LogLevel.Info,
        format = LogFormat.fromFunction((_, str) => str),
        destination = Paths.get(file)
      ) >>> Logging.withRootLoggerName("query-logger")

  private lazy val env = {
    logToFile match {
      case LogToFile.Full(file)        => Some(produceLoggerEnv(file))
      case LogToFile.GitFriendly(file) => Some(produceLoggerEnv(file))
      case LogToFile.Disabled          => None
    }
  }

  private[getquill] val runtime = env.map(Runtime.unsafeFromLayer(_))

  private[getquill] lazy val userProjectRootPath = Paths.get("").toAbsolutePath.toString + "/"

  def apply(queryString: String, sourcePath: String, line: Int, column: Int): Unit = {
    (logToFile, runtime) match {
      case (_: LogToFile.Full, Some(runtimeValue)) =>
        runtimeValue.unsafeRunAsync_(log.info(
          s"""
             |-- file: $sourcePath:$line:$column
             |-- time: ${ZonedDateTime.now().format(LogDatetimeFormatter.isoLocalDateTimeFormatter)}
             |$queryString;
             |""".stripMargin
        ))
      case (_: LogToFile.GitFriendly, Some(runtimeValue)) =>
        @inline def relativePath: String = sourcePath.replace(userProjectRootPath, "")

        runtimeValue.unsafeRunAsync_(log.info(
          s"""
             |-- file: $relativePath:$line:$column
             |$queryString;
             |""".stripMargin
        ))
      case (_, None) => // do nothing
    }
  }
}
