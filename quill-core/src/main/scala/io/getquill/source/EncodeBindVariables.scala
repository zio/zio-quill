package io.getquill.source

import io.getquill.util.Messages._
import io.getquill.ast._
import scala.reflect.macros.whitebox.Context

object EncodeBindVariables {

  def query[S](c: Context)(query: Query, bindingMap: collection.Map[Ident, (c.universe.ValDef, c.Tree)])(implicit s: c.WeakTypeTag[S]) = {
    val (bindedAction, bindingIdents) = BindVariables(query)(bindingMap.keys.toList)
    (bindedAction, encode(c)(bindingIdents, bindingMap))
  }

  def action[S](c: Context)(action: Action, bindingMap: collection.Map[Ident, (c.universe.ValDef, c.Tree)])(implicit s: c.WeakTypeTag[S]) = {
    val (bindedAction, bindingIdents) = BindVariables(action)(bindingMap.keys.toList)
    (bindedAction, encode(c)(bindingIdents, bindingMap))
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
