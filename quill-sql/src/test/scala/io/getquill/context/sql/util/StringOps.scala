package io.getquill.context.sql.util

object StringOps {

  implicit class StringOpsExt(str: String) {
    def collapseSpace: String = str.stripMargin.replaceAll("\\s+", " ").trim
  }
}
