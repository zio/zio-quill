package io.getquill.util

import java.io.ByteArrayOutputStream

object ExceptionOps {
  implicit final class ThrowableOpsMethods(private val t: Throwable) extends AnyVal {
    def stackTraceToString: String = {
      val stream = new ByteArrayOutputStream()
      val writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(stream))
      t.printStackTrace(new java.io.PrintWriter(writer))
      writer.flush()
      stream.toString
    }
  }
}
