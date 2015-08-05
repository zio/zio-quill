package io.getquill.sql

import scala.reflect.macros.whitebox.Context
import io.getquill.impl.Queryable
import io.getquill.norm.NormalizationMacro
import io.getquill.util.Messages
import io.getquill.util.Show._
import io.getquill.impl.Encoder
import io.getquill.impl.Quoted
import io.getquill.ast.Tuple

class SqlInsertMacro(val c: Context) extends NormalizationMacro with Messages {
  import c.universe._

  import SqlInsertShow._

  def insert[R, S, T](q: Expr[Quoted[Queryable[T]]])(values: Expr[Iterable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {
    val d = c.WeakTypeTag(c.prefix.tree.tpe)
    // TODO extractor is not needed
    val NormalizedQuery(query, extractor) = normalize(q.tree)(d, r, t)
    val sqlQuery = SqlQuery(query)
    val sql = sqlQuery.show
    info(sql)
    val bindings =
      sqlQuery.select match {
        case Tuple(_) =>
          for ((tpe, index) <- t.tpe.typeArgs.zipWithIndex) yield {
            tpe -> q"value.${TermName(s"_${index + 1}")}"
          }
        case other => List(t.tpe -> q"value")
      }
    val applyEncoders =
      for (((tpe, param), index) <- bindings.zipWithIndex) yield {
        val encoder =
          inferEncoder(tpe.erasure)(s)
            .getOrElse(fail(s"Source doesn't know how do encode $tpe"))
        q"r = $encoder($index, $param, r)"
      }
    val bind =
      q"""
        (row: $s, value: $t) => {
            var r = row
            $applyEncoders
            r
          }  
      """
    q"""
        ${c.prefix}.update($sql,  $values.map(v => (s: $s) => $bind(s, v)))
    """
  }

  private def inferEncoder[R](tpe: Type)(implicit r: WeakTypeTag[R]) = {
    def encoderType[T, R](implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) =
      c.weakTypeTag[Encoder[R, T]]
    inferImplicitValueWithFallback(encoderType(c.WeakTypeTag(tpe), r).tpe, c.prefix.tree.tpe, c.prefix.tree)
  }

}
