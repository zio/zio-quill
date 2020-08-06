package io.getquill.util

import io.getquill.AstPrinter

object Messages {

  private def variable(propName: String, envName: String, default: String) =
    Option(System.getProperty(propName)).orElse(sys.env.get(envName)).getOrElse(default)

  private[util] def prettyPrint = variable("quill.macro.log.pretty", "quill_macro_log", "false").toBoolean
  private[getquill] def alwaysAlias = variable("quill.query.alwaysAlias", "quill_query_alwaysAlias", "false").toBoolean
  private[getquill] def pruneColumns = variable("quill.query.pruneColumns", "quill_query_pruneColumns", "true").toBoolean
  private[util] def debugEnabled = variable("quill.macro.log", "quill_macro_log", "true").toBoolean
  private[util] def traceEnabled = variable("quill.trace.enabled", "quill_trace_enabled", "false").toBoolean
  private[util] def traceColors = variable("quill.trace.color", "quill_trace_color,", "false").toBoolean
  private[util] def traceOpinions = variable("quill.trace.opinion", "quill_trace_opinion", "false").toBoolean
  private[util] def traceAstSimple = variable("quill.trace.ast.simple", "quill_trace_ast_simple", "false").toBoolean
  private[util] def traces: List[TraceType] =
    variable("quill.trace.types", "quill_trace_types", "standard")
      .split(",")
      .toList
      .map(_.trim)
      .flatMap(trace => TraceType.values.filter(traceType => trace == traceType.value))

  def tracesEnabled(tt: TraceType) =
    traceEnabled && traces.contains(tt)

  sealed trait TraceType { def value: String }
  object TraceType {
    case object SqlNormalizations extends TraceType { val value = "sql" }
    case object Normalizations extends TraceType { val value = "norm" }
    case object Standard extends TraceType { val value = "standard" }
    case object NestedQueryExpansion extends TraceType { val value = "nest" }
    case object AvoidAliasConflict extends TraceType { val value = "alias" }
    case object ReifyLiftings extends TraceType { val value = "reify" }
    case object PatMatch extends TraceType { val value = "patmatch" }
    case object Quotation extends TraceType { val value = "quote" }
    case object RepropagateQuats extends TraceType { val value = "reprop" }
    case object RenameProperties extends TraceType { val value = "rename" }

    def values: List[TraceType] = List(Standard, SqlNormalizations, Normalizations, NestedQueryExpansion, AvoidAliasConflict, ReifyLiftings, PatMatch, Quotation, RepropagateQuats, RenameProperties)
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
}
