package io.getquill.context

import io.getquill.ast._
import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.util.OptionalTypecheck
import io.getquill.util.EnableReflectiveCalls

class QueryMacro(val c: MacroContext) extends ContextMacro {
  import c.universe.{ Ident => _, _ }

  def runQuery[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "executeQuery")

  def runQuerySingle[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandQuery[T](quoted, "executeQuerySingle")

  private def expandQuery[T](quoted: Tree, method: String)(implicit t: WeakTypeTag[T]) =
    OptionalTypecheck(c)(q"implicitly[${c.prefix}.Decoder[$t]]") match {
      case Some(decoder) => expandQueryWithDecoder(quoted, method, decoder)
      case None          => expandQueryWithMeta[T](quoted, method)
    }

  private def expandQueryWithDecoder(quoted: Tree, method: String, decoder: Tree) = {
    val ast = Map(extractAst(quoted), Ident("x"), Ident("x"))
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val expanded = ${expand(ast)}
        ${c.prefix}.${TermName(method)}(
          expanded.string,
          expanded.prepare,
          row => $decoder(0, row)
        )  
      """
    }
  }

  private def expandQueryWithMeta[T](quoted: Tree, method: String)(implicit t: WeakTypeTag[T]) = {
    val metaTpe = c.typecheck(tq"${c.prefix}.QueryMeta[$t]", c.TYPEmode).tpe
    val meta = c.inferImplicitValue(metaTpe).orElse(q"${c.prefix}.materializeQueryMeta[$t]")
    val ast = extractAst(c.typecheck(q"${c.prefix}.quote($meta.expand($quoted))"))
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val expanded = ${expand(ast)}
        ${c.prefix}.${TermName(method)}(
          expanded.string,
          expanded.prepare,
          $meta.extract
        )  
      """
    }
  }
}
