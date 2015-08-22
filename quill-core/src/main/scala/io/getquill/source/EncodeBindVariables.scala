package io.getquill.source

import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Action
import io.getquill.ast.Ident
import io.getquill.ast.Query
import io.getquill.util.Messages.RichContext
import io.getquill.ast.Ast

object EncodeBindVariables {

  def apply[S](c: Context)(ast: Ast, bindingMap: collection.Map[Ident, (c.universe.ValDef, c.Tree)])(implicit s: c.WeakTypeTag[S]) = {
    val (bindedAst, bindingIdents) = BindVariables(ast, bindingMap.keys.toList)
    (bindedAst, encode(c)(bindingIdents, bindingMap))
  }

  private def encode[S](c: Context)(bindingIdents: List[Ident], bindingMap: collection.Map[Ident, (c.universe.ValDef, c.Tree)])(implicit s: c.WeakTypeTag[S]) = {
    import c.universe._
    val applyEncoders =
      for ((ident, index) <- bindingIdents.zipWithIndex) yield {
        val (param, binding) = bindingMap(ident)
        val encoder =
          Encoding.inferEcoder(c)(param.tpt.tpe)(s)
            .getOrElse(c.fail(s"Source doesn't know how do encode '$param: ${param.tpt}'"))
        q"r = $encoder($index, $binding, r)"
      }
    q"""
      (row: $s) => {
        var r = row
        $applyEncoders
        r
      }  
    """
  }
}
