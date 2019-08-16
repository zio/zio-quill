package io.getquill

trait UpperCaseNonDefault extends NamingStrategy {
  override def column(s: String): String = s.toUpperCase
  override def table(s: String): String = s.toUpperCase
  override def default(s: String) = s
}
object UpperCaseNonDefault extends UpperCaseNonDefault
