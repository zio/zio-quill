package io.getquill.source

import scala.reflect.macros.whitebox.Context

import io.getquill.ast._
import io.getquill.util.Messages._

object EncodeBindVariables {

  def apply[S](c: Context)(ast: Ast, bindingMap: collection.Map[Ident, (c.Type, c.Tree)])(implicit s: c.WeakTypeTag[S]): (Ast, c.universe.Function) = {
    val (bindedAst, bindingIdents) = BindVariables(ast, bindingMap.keys.toList)
    (bindedAst, encode(c)(bindingIdents, bindingMap))
  }

  private def encode[S](c: Context)(bindingIdents: List[Ident], bindingMap: collection.Map[Ident, (c.Type, c.Tree)])(implicit s: c.WeakTypeTag[S]) = {
    import c.universe._
    val applyEncoders =
      for ((ident, index) <- bindingIdents.zipWithIndex) yield {
        val (tpe, binding) = bindingMap(ident)
        val encoder =
          Encoding.inferEcoder(c)(tpe)(s)
            .getOrElse(c.fail(s"Source doesn't know how do encode '$binding: $tpe'"))
        (row: Tree) => q"$encoder($index, $binding, $row)"
      }
    val body =
      applyEncoders.foldLeft[Tree](q"row") {
        case (row, function) =>
          function(row)
      }
    q"""
      (row: $s) => $body
    """
  }
}
