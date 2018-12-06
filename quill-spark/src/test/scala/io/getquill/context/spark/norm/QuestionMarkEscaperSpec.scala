package io.getquill.context.spark.norm

import io.getquill.Spec
import QuestionMarkEscaper._

class QuestionMarkEscaperSpec extends Spec {

  "should escape strings with question marks and even ones with slashes already" in {
    escape("foo ? bar \\? baz \\\\?") must equal("foo \\? bar \\\\? baz \\\\\\?")
  }

  "should escape and then unescape going back to original form" in {
    val str = "foo ? bar \\? baz \\\\?"
    unescape(escape(str)) must equal(str)
  }

  def plug1(str: String) = pluginValueSafe(str, "<1>")
  def plug2(str: String) = pluginValueSafe(plug1(str), "<2>")
  def plug3(str: String) = pluginValueSafe(plug2(str), "<3>")
  def plug4(str: String) = pluginValueSafe(plug3(str), "<4>")

  def plug2Q(str: String) = pluginValueSafe(plug1(str), "<2?>")
  def plug3QN(str: String) = pluginValueSafe(plug2Q(str), "<3>")
  def plug4Q(str: String) = pluginValueSafe(plug3QN(str), "<4?>")

  "should escape and replace variables correctly" in {
    val str = "foo ? bar ? ?"
    plug1(str) must equal("foo <1> bar ? ?")
    plug2(str) must equal("foo <1> bar <2> ?")
    plug3(str) must equal("foo <1> bar <2> <3>")
  }

  "should escape and replace variables correctly with other question marks" in {
    val str = "foo ? bar \\? ? baz ? \\\\? ?"
    plug1(str) must equal("foo <1> bar \\? ? baz ? \\\\? ?")
    plug2(str) must equal("foo <1> bar \\? <2> baz ? \\\\? ?")
    plug3(str) must equal("foo <1> bar \\? <2> baz <3> \\\\? ?")
    plug4(str) must equal("foo <1> bar \\? <2> baz <3> \\\\? <4>")
    unescape(plug4(str)) must equal("foo <1> bar ? <2> baz <3> \\? <4>")
  }

  "should escape and replace variables correctly even if the variables have question marks" in {
    val str = "foo ? bar \\? ? baz ? \\\\? ?"
    plug1(str) must equal("foo <1> bar \\? ? baz ? \\\\? ?")
    plug2Q(str) must equal("foo <1> bar \\? <2\\?> baz ? \\\\? ?")
    plug3QN(str) must equal("foo <1> bar \\? <2\\?> baz <3> \\\\? ?")
    plug4Q(str) must equal("foo <1> bar \\? <2\\?> baz <3> \\\\? <4\\?>")
    unescape(plug4Q(str)) must equal("foo <1> bar ? <2?> baz <3> \\? <4?>")
  }
}
