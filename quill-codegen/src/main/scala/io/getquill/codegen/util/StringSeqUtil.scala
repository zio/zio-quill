package io.getquill.codegen.util

object StringSeqUtil {
  implicit final class StringSeqExt(private val seq: Seq[String]) extends AnyVal {
    def pruneEmpty: Seq[String] = seq.filterNot(_.trim == "")
  }
}
