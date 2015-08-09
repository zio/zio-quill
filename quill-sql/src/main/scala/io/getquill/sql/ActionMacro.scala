package io.getquill.sql

import ActionShow._
import scala.reflect.macros.whitebox.Context
import QueryShow.sqlQueryShow
import io.getquill.impl.Queryable
import io.getquill.norm.NormalizationMacro
import io.getquill.util.Messages._
import io.getquill.ast.Ident
import io.getquill.util.Show.Shower
import io.getquill.encoding.Encoder
import io.getquill.impl.Actionable
import io.getquill.impl.Parser
import io.getquill.norm.Normalize
import io.getquill.util.InferImplicitValueWithFallback

class ActionMacro(val c: Context) extends Parser {
  import c.universe.{ Ident => _, _ }

  def run[R, S, T](q: Expr[Actionable[T]])(implicit r: WeakTypeTag[R], s: WeakTypeTag[S], t: WeakTypeTag[T]): Tree = {
    val action = Normalize(actionExtractor(q.tree))
    val sql = action.show
    c.info(sql)
    q"${c.prefix}.execute($sql)"
  }

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
    val (bindedAction, bindingIdents) = BindVariables(action)(bindingMap.keys.toList)
    val sql = bindedAction.show
    c.info(sql)
    val applyEncoders =
      for ((ident, index) <- bindingIdents.zipWithIndex) yield {
        val (param, binding) = bindingMap(ident)
        val encoder =
          inferEncoder(param.tpt.tpe)(s)
            .getOrElse(c.fail(s"Source doesn't know how do encode $param: ${param.tpt}"))
        q"r = $encoder($index, $binding, r)"
      }
    q"""
      { 
        val encode =
            $bindings.map(value => (row: $s) => {
              var r = row
              ..$applyEncoders
              r
            })
        ${c.prefix}.execute($sql, encode)
      }  
    """
  }

  private def inferEncoder[R](tpe: Type)(implicit r: WeakTypeTag[R]) = {
    def encoderType[T, R](implicit t: WeakTypeTag[T], r: WeakTypeTag[R]) =
      c.weakTypeTag[Encoder[R, T]]
    InferImplicitValueWithFallback(c)(encoderType(c.WeakTypeTag(tpe), r).tpe, c.prefix.tree)
  }
}
