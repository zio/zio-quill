package io.getquill.quotation

import io.getquill.ast.{ Ast, QuotedReference, RuntimeBinding, StatefulTransformer }

import scala.reflect.macros.whitebox.Context
import scala.collection.immutable.ListMap
import io.getquill.util.Messages._

case class Bindings[T](current: Option[T], bindings: ListMap[T, RuntimeBinding])

case class RuntimeBindings[T](c: Context, state: Bindings[T])
  extends StatefulTransformer[Bindings[T]] {

  override def apply(ast: Ast): (Ast, StatefulTransformer[Bindings[T]]) =
    ast match {
      case quoted: QuotedReference[T] =>
        RuntimeBindings(c, Bindings(Some(quoted.tree), state.bindings)).apply(quoted.ast)
      case binding: RuntimeBinding =>
        state.current match {
          case None => c.fail(s"Please report a bug. The binding '${binding.toString}' expected a Quoted Reference")
          case Some(current) =>
            val bindings = Bindings(state.current, state.bindings + ((current, binding)))
            (binding, RuntimeBindings(c, bindings))
        }
      case other =>
        super.apply(other)
    }
}

object RuntimeBindings {
  def apply(c: Context)(ast: Ast): ListMap[c.Tree, RuntimeBinding] =
    RuntimeBindings(c, Bindings(None, ListMap.empty[c.Tree, RuntimeBinding]))(ast) match {
      case (_, transformer) =>
        transformer.state.bindings
    }
}
