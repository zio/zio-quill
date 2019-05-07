package io.getquill.codegen.util

object StringSeqUtil {
  implicit class StringSeqExt(seq: Seq[String]) {
    def pruneEmpty = seq.filterNot(_.trim == "")
  }
}
