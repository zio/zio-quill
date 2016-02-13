package io.getquill.sources

import scala.reflect.macros.whitebox.Context

import io.getquill.ast._
import io.getquill.util.Messages._

object EncodeParams {

  def apply[S](c: Context)(params: collection.Map[Ident, (c.Type, c.Tree)])(implicit s: c.WeakTypeTag[S]): c.Tree =
    raw[S](c) {
      params.map {
        case (ident, (tpe, tree)) =>
          val encoder =
            Encoding.inferEncoder(c)(tpe)(s)
              .getOrElse(c.fail(s"Source doesn't know how do encode '$ident: $tpe'"))
          (ident, (encoder, tree))
      }.toMap
    }

  def raw[S](c: Context)(params: collection.Map[Ident, (c.Tree, c.Tree)])(implicit s: c.WeakTypeTag[S]): c.Tree = {
    import c.universe._
    val encoders =
      for ((ident, (encoder, tree)) <- params) yield {
        q"${ident.name} -> ((row: $s, index: Int) => $encoder(index, $tree, row))"
      }
    q"""
    {
      val bindingMap = collection.Map(..$encoders)
      (bindings: List[String]) =>
        (row: $s) =>
          bindings.foldLeft((row, 0)) {
            case ((row, index), binding) =>
              (bindingMap(binding)(row, index), index + 1)
          }._1
    }
    """
  }
}
