package io.getquill.sql

import ActionShow._
import scala.reflect.macros.whitebox.Context
import SqlQueryShow.sqlQueryShow
import io.getquill.impl.Queryable
import io.getquill.norm.NormalizationMacro
import io.getquill.util.Messages
import io.getquill.ast.Ident
import io.getquill.util.Show.Shower
import io.getquill.impl.Encoder
import io.getquill.impl.Actionable
import io.getquill.impl.Parser
import io.getquill.util.ImplicitResolution
import io.getquill.norm.Normalize

class SqlActionMacro(val c: Context) extends Parser with Messages with ImplicitResolution {
  import c.universe.{ Ident => _, _ }

  def run[R, S, T](q: Expr[Actionable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree =
    run[R, S, T](q.tree, List(), q"List[Unit]()")

  def run1[P1, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](q: Expr[P1 => Actionable[T]])(bindings: Expr[Iterable[P1]]): Tree =
    runParametrized[R, S, T](q.tree, bindings)

  def run2[P1, P2, R: WeakTypeTag, S: WeakTypeTag, T: WeakTypeTag](q: Expr[(P1, P2) => Actionable[T]])(bindings: Expr[Iterable[(P1, P2)]]): Tree =
    runParametrized[R, S, T](q.tree, bindings)

  private def runParametrized[R, S, T](q: Tree, bindings: Expr[Iterable[Any]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) =
    q match {
      case q"(..$params) => $body" =>
        run[R, S, T](body, params, bindings.tree)
    }

  private def run[R, S, T](q: Tree, params: List[ValDef], bindings: Tree)(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]) = {
    val action = Normalize(actionExtractor(q))
    val bindingMap: Map[Ident, (ValDef, Tree)] =
      (for ((param, index) <- params.zipWithIndex) yield {
        identExtractor(param) -> (param, q"value.${TermName(s"_${index + 1}")}")
      }).toMap
    val (bindedAction, bindingIdents) = ReplaceBindVariables(action)(bindingMap.keys.toList)
    val sql = bindedAction.show
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
          ${c.prefix}.execute($sql, $bindings.map(value => (row: $s) => {
            var r = row
            ..$applyEncoders
            r
          }))
      }  
    """
  }

  private def inferEncoder[R](tpe: Type)(implicit r: WeakTypeTag[R]) = {
    def encoderType[T, R](implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) =
      c.weakTypeTag[Encoder[R, T]]
    inferImplicitValueWithFallback(encoderType(c.WeakTypeTag(tpe), r).tpe, c.prefix.tree.tpe, c.prefix.tree)
  }
}
