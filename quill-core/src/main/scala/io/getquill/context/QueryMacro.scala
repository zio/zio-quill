package io.getquill.context

import io.getquill.ast._
import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.util.OptionalTypecheck
import io.getquill.util.EnableReflectiveCalls

class QueryMacro(val c: MacroContext) extends ContextMacro {
  import c.universe.{ Ident => _, _ }

  sealed trait FetchSizeBehavior
  case class UsesExplicitFetch(tree: Tree) extends FetchSizeBehavior
  case object UsesDefaultFetch extends FetchSizeBehavior
  case object DoesNotUseFetch extends FetchSizeBehavior

  def streamQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "streamQuery", UsesDefaultFetch)

  def streamQueryFetch[T](quoted: Tree, fetchSize: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "streamQuery", UsesExplicitFetch(fetchSize))

  def runQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "executeQuery", DoesNotUseFetch)

  def runQuerySingle[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "executeQuerySingle", DoesNotUseFetch)

  def translateQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "translateQuery", DoesNotUseFetch)

  def bindQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "bindQuery", DoesNotUseFetch)

  private def expandQuery[T](quoted: Tree, method: String, fetchBehavior: FetchSizeBehavior)(implicit t: WeakTypeTag[T]) =
    OptionalTypecheck(c)(q"implicitly[${c.prefix}.Decoder[$t]]") match {
      case Some(decoder) => expandQueryWithDecoder(quoted, method, decoder, fetchBehavior)
      case None          => expandQueryWithMeta[T](quoted, method, fetchBehavior)
    }

  private def expandQueryWithDecoder(quoted: Tree, method: String, decoder: Tree, fetchBehavior: FetchSizeBehavior) = {
    val ast = Map(extractAst(quoted), Ident("x"), Ident("x"))
    val invocation =
      fetchBehavior match {
        case UsesExplicitFetch(size) =>
          q"""
            ${c.prefix}.${TermName(method)}(
              Some(${size}),
              expanded.string,
              expanded.prepare,
              row => $decoder(0, row)
            )
           """
        case UsesDefaultFetch =>
          q"""
            ${c.prefix}.${TermName(method)}(
              None,
              expanded.string,
              expanded.prepare,
              row => $decoder(0, row)
            )
           """
        case DoesNotUseFetch =>
          q"""
            ${c.prefix}.${TermName(method)}(
              expanded.string,
              expanded.prepare,
              row => $decoder(0, row)
            )
           """
      }

    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val expanded = ${expand(ast)}
        ${invocation}
      """
    }
  }

  private def expandQueryWithMeta[T](quoted: Tree, method: String, fetch: FetchSizeBehavior)(implicit t: WeakTypeTag[T]) = {
    val metaTpe = c.typecheck(tq"${c.prefix}.QueryMeta[$t]", c.TYPEmode).tpe
    val meta = c.inferImplicitValue(metaTpe).orElse(q"${c.prefix}.materializeQueryMeta[$t]")
    val ast = extractAst(c.typecheck(q"${c.prefix}.quote($meta.expand($quoted))"))
    val invocation =
      fetch match {
        case UsesExplicitFetch(size) =>
          q"""
            ${c.prefix}.${TermName(method)}(
              Some(${size}),
              expanded.string,
              expanded.prepare,
              $meta.extract
            )
           """
        case UsesDefaultFetch =>
          q"""
            ${c.prefix}.${TermName(method)}(
              None,
              expanded.string,
              expanded.prepare,
              $meta.extract
            )
           """
        case DoesNotUseFetch =>
          q"""
            ${c.prefix}.${TermName(method)}(
              expanded.string,
              expanded.prepare,
              $meta.extract
            )
           """
      }

    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val expanded = ${expand(ast)}
        ${invocation}
      """
    }
  }
}
