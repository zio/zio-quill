package io.getquill.context.sql.util

object StringOps {

  implicit final class StringOpsExt(private val str: String) extends AnyVal {
    def collapseSpace: String =
      str.stripMargin.replaceAll("\\s+", " ").trim
  }
}
