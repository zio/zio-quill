package io.getquill.util

import io.getquill.AstPrinter
import io.getquill.idiom.Idiom
import io.getquill.util.IndentUtil._

import scala.reflect.macros.blackbox.{ Context => MacroContext }

object Messages {

  private def variable(propName: String, envName: String, default: String) =
    Option(System.getProperty(propName)).orElse(sys.env.get(envName)).getOrElse(default)

  private[util] val prettyPrint = variable("quill.macro.log.pretty", "quill_macro_log", "false").toBoolean
  private[util] val debugEnabled = variable("quill.macro.log", "quill_macro_log", "true").toBoolean
  private[util] val traceEnabled = variable("quill.trace.enabled", "quill_trace_enabled", "false").toBoolean
  private[util] val traceColors = variable("quill.trace.color", "quill_trace_color,", "false").toBoolean
  private[util] val traceOpinions = variable("quill.trace.opinion", "quill_trace_opinion", "false").toBoolean
  private[util] val traceAstSimple = variable("quill.trace.ast.simple", "quill_trace_ast_simple", "false").toBoolean
  private[util] val traces: List[TraceType] =
    variable("quill.trace.types", "quill_trace_types", "standard")
      .split(",")
      .toList
      .map(_.trim)
      .flatMap(trace => TraceType.values.filter(traceType => trace == traceType.value))

  def tracesEnabled(tt: TraceType) =
    traceEnabled && traces.contains(tt)

  sealed trait TraceType { def value: String }
  object TraceType {
    case object Normalizations extends TraceType { val value = "norm" }
    case object Standard extends TraceType { val value = "standard" }
    case object NestedQueryExpansion extends TraceType { val value = "nest" }
    case object AvoidAliasConflict extends TraceType { val value = "alias" }

    def values: List[TraceType] = List(Standard, Normalizations, NestedQueryExpansion, AvoidAliasConflict)
  }

  val qprint = new AstPrinter(traceOpinions, traceAstSimple)

  def fail(msg: String) =
    throw new IllegalStateException(msg)

  def title[T](label: String, traceType: TraceType = TraceType.Standard) =
    trace[T](("=".repeat(10)) + s" $label " + ("=".repeat(10)), 0, traceType)

  def trace[T](label: String, numIndent: Int = 0, traceType: TraceType = TraceType.Standard) =
    (v: T) =>
      {
        val indent = (0 to numIndent).map(_ => "").mkString("  ")
        if (tracesEnabled(traceType))
          println(s"$indent$label\n${
            {
              if (traceColors) qprint.apply(v).render else qprint.apply(v).plainText
            }.split("\n").map(s"$indent  " + _).mkString("\n")
          }")
        v
      }

  implicit class StringExt(str: String) {
    def repeat(n: Int) = (0 until n).map(_ => str).mkString
  }

  implicit class RichContext(c: MacroContext) {

    def error(msg: String): Unit =
      c.error(c.enclosingPosition, msg)

    def fail(msg: String): Nothing =
      c.abort(c.enclosingPosition, msg)

    def warn(msg: String): Unit =
      c.warning(c.enclosingPosition, msg)

    def query(queryString: String, idiom: Idiom): Unit = {
      val formatted = if (prettyPrint) idiom.format(queryString) else queryString
      val output =
        if (formatted.fitsOnOneLine)
          formatted
        else
          "\n" + formatted.multiline(1, "| ") + "\n\n"

      if (debugEnabled) c.info(c.enclosingPosition, output, force = true)
    }

    def info(msg: String): Unit =
      if (debugEnabled) c.info(c.enclosingPosition, msg, force = true)

    def debug[T](v: T): T = {
      info(v.toString)
      v
    }
  }
}
