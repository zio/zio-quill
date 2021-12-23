package io.getquill.codegen.util

object StringSeqUtil {
  implicit class StringSeqExt(seq: Seq[String]) {
    def pruneEmpty: Seq[String] = seq.filterNot(_.trim == "")
  }
}
