package io.getquill.sql

import scala.reflect.macros.whitebox.Context
import io.getquill.impl.Queryable
import io.getquill.norm.NormalizationMacro
import io.getquill.util.Messages
import io.getquill.util.Show.Shower
import io.getquill.impl.Encoder

class SqlDeleteMacro(val c: Context) extends NormalizationMacro with Messages {
  import c.universe._
  
  import SqlDeleteShow._

  def delete[R, S, T](q: Expr[Queryable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    delete[R, S, T](q.tree, List(), List())

  def delete1[P1, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](q: Expr[P1 => Queryable[T]])(p1: Expr[P1]): Tree =
    deleteParametrized[R, S, T](q.tree, List(p1))

  def delete2[P1, P2, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](q: Expr[(P1, P2) => Queryable[T]])(p1: Expr[P1], p2: Expr[P2]): Tree =
    deleteParametrized[R, S, T](q.tree, List(p1, p2))

  private def deleteParametrized[R, S, T](q: Tree, bindings: List[Expr[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) =
    q match {
      case q"(..$params) => $body" =>
        delete[R, S, T](body, params, bindings)
    }

  private def delete[R, S, T](q: Tree, params: List[ValDef], bindings: List[Expr[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) = {
    val bindingMap =
      (for ((param, binding) <- params.zip(bindings)) yield {
        identExtractor(param) -> (param, binding)
      }).toMap
    val d = c.WeakTypeTag(c.prefix.tree.tpe)
    val NormalizedQuery(query, extractor) = normalize(q)(d, r, t)
    val (bindedQuery, bindingIdents) = ReplaceBindVariables(query)(bindingMap.keys.toList)
    val sql = SqlQuery(bindedQuery).show
    info(sql)
    val applyEncoders =
      for ((ident, index) <- bindingIdents.zipWithIndex) yield {
        val (param, binding) = bindingMap(ident)
        val encoder =
          inferEncoder(param.tpt.tpe)(s)
            .getOrElse(fail(s"Source doesn't know how do encode $param: ${param.tpt}"))
        q"r = $encoder($index, $binding, r)"
      }
    q"""
      {
          def encode(row: $s) = {
            var r = row
            $applyEncoders
            r
          }
          ${c.prefix}.update($sql, List(encode _))
      }  
    """
  }

  private def inferEncoder[R](tpe: Type)(implicit r: WeakTypeTag[R]) = {
    def encoderType[T, R](implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) =
      c.weakTypeTag[Encoder[R, T]]
    inferImplicitValueWithFallback(encoderType(c.WeakTypeTag(tpe), r).tpe, c.prefix.tree.tpe, c.prefix.tree)
  }

  private def interpret[R, T](q: Tree)(implicit r: WeakTypeTag[R], t: WeakTypeTag[T]) = {
    val d = c.WeakTypeTag(c.prefix.tree.tpe)
    val NormalizedQuery(query, extractor) = normalize(q)(d, r, t)
    (SqlQuery(query).show, extractor)
  }
}
