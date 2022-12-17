package io.getquill.context

import io.getquill.ast._
import io.getquill.quat.Quat

import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.util.{ EnableReflectiveCalls, Messages, OptionalTypecheck }

class QueryMacro(val c: MacroContext) extends ContextMacro {
  import c.universe.{ Ident => _, _ }

  sealed trait FetchSizeArg
  case class UsesExplicitFetch(tree: Tree) extends FetchSizeArg
  case object UsesDefaultFetch extends FetchSizeArg
  case object DoesNotUseFetch extends FetchSizeArg

  sealed trait PrettyPrintingArg
  case class ExplicitPrettyPrint(tree: Tree) extends PrettyPrintingArg
  case object DefaultPrint extends PrettyPrintingArg

  sealed trait ContextMethod { def name: String }
  case class StreamQuery(fetchSizeBehavior: FetchSizeArg) extends ContextMethod { val name = "streamQuery" }
  case object ExecuteQuery extends ContextMethod { val name = "executeQuery" }
  case object ExecuteQuerySingle extends ContextMethod { val name = "executeQuerySingle" }
  case class TranslateQuery(prettyPrintingArg: PrettyPrintingArg) extends ContextMethod { val name = "translateQuery" }
  case object PrepareQuery extends ContextMethod { val name = "prepareQuery" }

  def streamQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, StreamQuery(UsesDefaultFetch))

  def streamQueryFetch[T](quoted: Tree, fetchSize: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, StreamQuery(UsesExplicitFetch(fetchSize)))

  def runQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, ExecuteQuery)

  def runQuerySingle[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, ExecuteQuerySingle)

  def translateQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, TranslateQuery(DefaultPrint))

  def translateQueryPrettyPrint[T](quoted: Tree, prettyPrint: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, TranslateQuery(ExplicitPrettyPrint(prettyPrint)))

  def prepareQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, PrepareQuery)

  private def expandQuery[T](quoted: Tree, method: ContextMethod)(implicit t: WeakTypeTag[T]) = {
    val topLevelQuat = inferQuat(t.tpe)
    OptionalTypecheck(c)(q"implicitly[${c.prefix}.Decoder[$t]]") match {
      case Some(decoder) => expandQueryWithDecoder(quoted, method, decoder, topLevelQuat)
      case None          => expandQueryWithMeta[T](quoted, method, topLevelQuat)
    }
  }

  private def expandQueryWithDecoder(quoted: Tree, method: ContextMethod, decoder: Tree, topLevelQuat: Quat) = {
    val extractedAst = extractAst(quoted)
    val ast = Map(extractedAst, Ident("x", extractedAst.quat), Ident("x", extractedAst.quat))
    val invocation =
      method match {
        case StreamQuery(UsesExplicitFetch(size)) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              Some(${size}),
              expanded.string,
              expanded.prepare,
              (row, session) => $decoder(0, row, session)
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case StreamQuery(UsesDefaultFetch) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              None,
              expanded.string,
              expanded.prepare,
              (row, session) => $decoder(0, row, session)
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case StreamQuery(DoesNotUseFetch) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare,
              (row, session) => $decoder(0, row, session)
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case TranslateQuery(ExplicitPrettyPrint(argValue)) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare,
              (row, session) => $decoder(0, row, session),
              prettyPrint = ${argValue}
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case TranslateQuery(DefaultPrint) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare,
              (row, session) => $decoder(0, row, session),
              prettyPrint = false
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case PrepareQuery =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case _ =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare,
              (row, session) => $decoder(0, row, session)
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
      }

    val liftUnlift = new { override val mctx: c.type = c } with TokenLift(0) // Don't serialize quats for the top-level
    val liftQuat: Liftable[Quat] = liftUnlift.quatLiftable
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val staticTopLevelQuat = ${if (Messages.attachTopLevelQuats) liftQuat(topLevelQuat) else q"io.getquill.quat.Quat.Unknown"}
        val (idiomContext, expanded) = ${expand(ast, topLevelQuat)}
        ${invocation}
      """
    }
  }

  private def expandQueryWithMeta[T](quoted: Tree, method: ContextMethod, topLevelQuat: Quat)(implicit t: WeakTypeTag[T]) = {
    val metaTpe = c.typecheck(tq"${c.prefix}.QueryMeta[$t]", c.TYPEmode).tpe
    val meta = c.inferImplicitValue(metaTpe).orElse(q"${c.prefix}.materializeQueryMeta[$t]")
    val ast = extractAst(c.typecheck(q"${c.prefix}.quote($meta.expand($quoted))"))
    val invocation =
      method match {
        case StreamQuery(UsesExplicitFetch(size)) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              Some(${size}),
              expanded.string,
              expanded.prepare,
              $meta.extract
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case StreamQuery(UsesDefaultFetch) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              None,
              expanded.string,
              expanded.prepare,
              $meta.extract
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case StreamQuery(DoesNotUseFetch) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare,
              $meta.extract
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case TranslateQuery(ExplicitPrettyPrint(argValue)) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare,
              $meta.extract,
              prettyPrint = ${argValue}
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case TranslateQuery(DefaultPrint) =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare,
              $meta.extract,
              prettyPrint = false
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case PrepareQuery =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
        case _ =>
          q"""
            ${c.prefix}.${TermName(method.name)}(
              expanded.string,
              expanded.prepare,
              $meta.extract
            )(io.getquill.context.ExecutionInfo(expanded.executionType, expanded.ast, staticTopLevelQuat), ())
           """
      }

    val liftUnlift = new { override val mctx: c.type = c } with TokenLift(0) // Don't serialize quats for the top level
    val liftQuat: Liftable[Quat] = liftUnlift.quatLiftable
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val staticTopLevelQuat = ${if (Messages.attachTopLevelQuats) liftQuat(topLevelQuat) else q"io.getquill.quat.Quat.Unknown"}
        val (idiomContext, expanded) = ${expand(ast, topLevelQuat)}
        ${invocation}
      """
    }
  }
}
