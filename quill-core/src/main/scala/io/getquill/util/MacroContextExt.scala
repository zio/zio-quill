package io.getquill.util

import io.getquill.idiom.Idiom
import io.getquill.quat.VerifyNoBranches
import io.getquill.util.IndentUtil._
import io.getquill.util.Messages.{debugEnabled, errorPrefix, prettyPrint}

import java.util.concurrent.Executors
import scala.concurrent.{ExecutionContext, Future, blocking}
import scala.reflect.macros.blackbox.{Context => MacroContext}

object MacroContextExt {

  /**
   * Using only 1 thread to keep the ordering of messages.
   */
  private implicit val loggingExecutor: ExecutionContext =
    ExecutionContext.fromExecutor(Executors.newFixedThreadPool(1))

  private[getquill] val queryLogger: QueryLogger = new QueryLogger(Messages.quillLogFile)

  implicit final class RichContext(private val c: MacroContext) extends AnyVal {

    def error(msg: String): Unit =
      fireAndForget {
        c.error(c.enclosingPosition, if (errorPrefix) s"[quill] $msg" else msg)
      }

    def fail(msg: String): Unit =
      fireAndForget {
        c.abort(c.enclosingPosition, if (errorPrefix) s"[quill] $msg" else msg)
      }

    def warn(msg: String): Unit =
      fireAndForget {
        c.warning(c.enclosingPosition, msg)
      }

    def warn(verifyOutput: VerifyNoBranches.Output): Unit =
      fireAndForget {
        val pos            = c.enclosingPosition
        val locationString = s"${pos.source.path}:${pos.line}:${pos.column}"
        verifyOutput.messages.distinct.foreach(message =>
          println(s"[WARNING] ${locationString} Questionable row-class found.\n${message.msg}")
        )
      }

    def query(queryString: String, idiom: Idiom): Unit =
      fireAndForget {
        val formatted = if (prettyPrint) idiom.format(queryString) else queryString
        val output =
          if (formatted.fitsOnOneLine) formatted
          else "\n" + formatted.multiline(1, "| ") + "\n\n"

        queryLogger(output, c.enclosingPosition.source.path, c.enclosingPosition.line, c.enclosingPosition.column)

        if (debugEnabled) c.info(c.enclosingPosition, output, force = true)
      }

    def info(msg: String): Unit =
      if (debugEnabled) fireAndForget(c.info(c.enclosingPosition, msg, force = true))

    def debug[T](v: T): T = {
      info(v.toString)
      v
    }

    private def fireAndForget(f: => Unit): Unit = Future(blocking(f))(loggingExecutor)
  }
}
