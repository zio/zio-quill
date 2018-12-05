package io.getquill.context.spark.norm

import java.util.regex.Matcher

object QuestionMarkEscaper {
  def escape(str: String) = str.replace("?", "\\?")
  def unescape(str: String) = str.replace("\\?", "?")

  def pluginValueSafe(str: String, value: String) =
    str.replaceFirst("(?<!\\\\)\\?", Matcher.quoteReplacement(escape(value)))
}
