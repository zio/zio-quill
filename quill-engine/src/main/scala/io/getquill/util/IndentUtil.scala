package io.getquill.util

object IndentUtil {
  implicit class StringOpsExt(str: String) {
    def fitsOnOneLine: Boolean = !str.contains("\n")
    def multiline(indent: Int, prefix: String): String =
      str.split("\n").map(elem => indent.prefix + prefix + elem).mkString("\n")
  }

  implicit class IndentOps(i: Int) {
    def prefix = indentOf(i)
  }

  private def indentOf(num: Int) =
    (0 to num).map(_ => "").mkString("  ")
}
