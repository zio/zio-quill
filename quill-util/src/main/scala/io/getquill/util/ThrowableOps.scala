package io.getquill.util

import java.io.ByteArrayOutputStream

object ThrowableOps {
  implicit class ThrowableOpsMethods(t: Throwable) {
    def stackTraceToString: String = {
      val stream = new ByteArrayOutputStream()
      val writer = new java.io.BufferedWriter(new java.io.OutputStreamWriter(stream))
      t.printStackTrace(new java.io.PrintWriter(writer))
      writer.flush
      stream.toString
    }
  }
}
