package io.getquill.util

import io.getquill.util.Messages.{ LogToFile, quillLogFile }
import zio._
import zio.clock.Clock
import zio.console.Console
import zio.logging.LogAppender.Service
import zio.logging.Logging.{ addTimestamp, modifyLoggerM }
import zio.logging._

import java.io.{ BufferedWriter, FileOutputStream, OutputStreamWriter, Writer }
import java.nio.charset.{ Charset, StandardCharsets }
import java.nio.file.{ Path, Paths }
import java.time.ZonedDateTime

class AppendingLogWriter(
  destination:        Path,
  charset:            Charset,
  autoFlushBatchSize: Int,
  bufferedIOSize:     Option[Int]
) extends Writer {
  private val writer: Writer = {
    val output = new OutputStreamWriter(new FileOutputStream(destination.toFile, true), charset)
    bufferedIOSize match {
      case Some(bufferSize) => new BufferedWriter(output, bufferSize)
      case None             => output
    }
  }

  private var entriesWritten: Long = 0

  def write(buffer: Array[Char], offset: Int, length: Int): Unit =
    writer.write(buffer, offset, length)

  def writeln(line: String): Unit = {
    writer.write(line)
    writer.write(System.lineSeparator)

    entriesWritten += 1

    if (entriesWritten % autoFlushBatchSize == 0)
      writer.flush()
  }

  def flush(): Unit = writer.flush()

  def close(): Unit = writer.close()
}

class QueryLogger(logToFile: LogToFile) {

  def appendFile[A](
    destination:        Path,
    charset:            Charset,
    autoFlushBatchSize: Int,
    bufferedIOSize:     Option[Int],
    format0:            LogFormat[A]
  )(implicit tag: Tag[LogAppender.Service[A]]): ZLayer[Any, Throwable, Appender[A]] =
    ZManaged
      .fromAutoCloseable(UIO(new AppendingLogWriter(destination, charset, autoFlushBatchSize, bufferedIOSize)))
      .zip(ZRef.makeManaged(false))
      .map {
        case (writer, hasWarned) =>
          new Service[A] {
            override def write(ctx: LogContext, msg: => A): UIO[Unit] =
              Task(writer.writeln(format0.format(ctx, msg))).catchAll { t =>
                UIO {
                  System.err.println(
                    s"Logging to file $destination failed with an exception. Further exceptions will be suppressed in order to prevent log spam."
                  )
                  t.printStackTrace(System.err)
                }.unlessM(hasWarned.getAndSet(true))
              }
          }
      }
      .toLayer[LogAppender.Service[A]]

  def logFile(
    destination:        Path,
    charset:            Charset           = StandardCharsets.UTF_8,
    autoFlushBatchSize: Int               = 1,
    bufferedIOSize:     Option[Int]       = None,
    logLevel:           LogLevel          = LogLevel.Info,
    format:             LogFormat[String] = LogFormat.SimpleConsoleLogFormat((_, s) => s)
  ): ZLayer[Console with Clock, Throwable, Logging] =
    (ZLayer.requires[Clock] ++
      appendFile[String](destination, charset, autoFlushBatchSize, bufferedIOSize, format)
      .map(appender => Has(appender.get.filter((ctx, _) => ctx.get(LogAnnotation.Level) >= logLevel)))
      >+> Logging.make >>> modifyLoggerM(addTimestamp[String]))

  def produceLoggerEnv(file: String) =
    ZEnv.live >>>
      logFile(
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

  def apply(queryString: String, sourcePath: String, line: Int, column: Int): Unit = {
    runtime match {
      case Some(runtimeValue) =>
        runtimeValue.unsafeRunAsync_(log.info(
          s"""
             |-- file: $sourcePath:$line:$column
             |-- time: ${ZonedDateTime.now().format(LogDatetimeFormatter.isoLocalDateTimeFormatter)}
             |$queryString;
             |""".stripMargin
        ))
      case None => // do nothing
    }
  }
}
