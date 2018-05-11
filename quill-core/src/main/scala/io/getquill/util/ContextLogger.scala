package io.getquill.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

import scala.annotation.tailrec

class ContextLogger(name: String) {
  val underlying = Logger(LoggerFactory.getLogger(name))

  private val bindsEnabled = sys.props.get("quill.binds.log").contains("true")
  private val nullToken = "null"

  def logQuery(query: String, params: Seq[Any]): Unit = {
    if (!bindsEnabled || params.isEmpty) underlying.debug(query)
    else {
      underlying.debug("{} - binds: {}", query, prepareParams(params))
    }
  }

  def logBatchItem(query: String, params: Seq[Any]): Unit = {
    if (bindsEnabled) {
      underlying.debug("{} - batch item: {}", query, prepareParams(params))
    }
  }

  private def prepareParams(params: Seq[Any]): String = params
    .reverse
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

