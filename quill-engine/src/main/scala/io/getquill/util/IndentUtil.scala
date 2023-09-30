package io.getquill.util

object IndentUtil {
  implicit final class StringOpsExt(private val str: String) extends AnyVal {
    def fitsOnOneLine: Boolean = !str.contains("\n")
    def multiline(indent: Int, prefix: String): String =
      str.split("\n").map(elem => indent.prefix + prefix + elem).mkString("\n")
  }

  implicit final class IndentOps(private val i: Int) extends AnyVal  {
    def prefix: String = indentOf(i)
  }

  private def indentOf(num: Int): String =
    (0 to num).map(_ => "").mkString("  ")
}
