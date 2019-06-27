package io.getquill.util

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

object TimeLogger {
  val logger = Logger(LoggerFactory.getLogger("executeQuery"))

  def log[T](f: => T): T = {
    val sec = System.currentTimeMillis
    val res = f
    logger.info(s"Execution Query Time: ${sec - System.currentTimeMillis()} ms")
    res
  }
}