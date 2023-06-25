package io.getquill.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

class ContextLogger(name: String) {
  val underlying = Logger(LoggerFactory.getLogger(name))

  private def bindsEnabled = io.getquill.util.Messages.logBinds || underlying.underlying.isTraceEnabled
  private val nullToken    = "null"
  private def maxQueryLen  = Messages.queryTooLongForLogs

  private implicit class TrimQueryOps(str: String) {
    def trimTooLong = trimQuery(str)
  }

  private def trimQuery(query: String) =
    if (maxQueryLen > 0)
      query.take(maxQueryLen) + (if (query.length > maxQueryLen) "..." else "")
    else
      query

  def logQuery(msg: String, query: String): Unit =
    underlying.debug(s"${msg}:{}", query.trimTooLong)

  def logQuery(query: String, params: Seq[Any]): Unit =
    if (!bindsEnabled || params.isEmpty) underlying.debug(query.trimTooLong)
    else {
      underlying.debug("{} - binds: {}", query.trimTooLong, prepareParams(params))
    }

  def logBatchItem(query: String, params: Seq[Any]): Unit =
    if (bindsEnabled) {
      underlying.debug("{} - batch item: {}", query.trimTooLong, prepareParams(params))
    }

  private def prepareParams(params: Seq[Any]): String = params.reverse
    .map(prepareParam)
    .mkString("[", ", ", "]")

  @tailrec
  private def prepareParam(param: Any): String = param match {
    case None | null => nullToken
    case Some(x)     => prepareParam(x)
    case str: String => s"'$str'"
    case _           => param.toString
  }
}

object ContextLogger {
  def apply(ctxClass: Class[_]): ContextLogger = new ContextLogger(ctxClass.getName)
}
