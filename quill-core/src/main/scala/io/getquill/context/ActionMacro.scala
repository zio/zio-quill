package io.getquill.context

import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.ast._
import io.getquill.quotation.ReifyLiftings
import io.getquill.util.Messages._
import io.getquill.norm.BetaReduction
import io.getquill.util.EnableReflectiveCalls

class ActionMacro(val c: MacroContext)
  extends ContextMacro
  with ReifyLiftings {
  import c.universe.{ Ident => _, Function => _, _ }

  def runAction(quoted: Tree): Tree =
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val expanded = ${expand(extractAst(quoted))}
        ${c.prefix}.executeAction(
          expanded.string,
          expanded.prepare
        )
      """
    }

  def runActionReturning[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val expanded = ${expand(extractAst(quoted))}
        ${c.prefix}.executeActionReturning(
          expanded.string,
          expanded.prepare,
          ${returningExtractor[T]},
          $returningColumn
        )
      """
    }

  def runBatchAction(quoted: Tree): Tree =
    expandBatchAction(quoted) {
      case (batch, param, expanded) =>
        q"""
          ..${EnableReflectiveCalls(c)}
          ${c.prefix}.executeBatchAction(
            $batch.map { $param => 
              val expanded = $expanded
              (expanded.string, expanded.prepare)
            }.groupBy(_._1).map {
              case (string, items) =>
                ${c.prefix}.BatchGroup(string, items.map(_._2).toList)
            }.toList
          )
        """
    }

  def runBatchActionReturning[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandBatchAction(quoted) {
      case (batch, param, expanded) =>
        q"""
          ..${EnableReflectiveCalls(c)}
          ${c.prefix}.executeBatchActionReturning(
            $batch.map { $param => 
              val expanded = $expanded
              ((expanded.string, $returningColumn), expanded.prepare)
            }.groupBy(_._1).map {
              case ((string, column), items) =>
                ${c.prefix}.BatchGroupReturning(string, column, items.map(_._2).toList)
            }.toList,
            ${returningExtractor[T]}
          )
        """
    }

  def expandBatchAction(quoted: Tree)(call: (Tree, Tree, Tree) => Tree): Tree =
    BetaReduction(extractAst(quoted)) match {
      case ast @ Foreach(lift: Lift, alias, body) =>
        val batch = lift.value.asInstanceOf[Tree]
        val batchItemType = batch.tpe.typeArgs.head
        c.typecheck(q"(value: $batchItemType) => value") match {
          case q"($param) => $value" =>
            val nestedLift =
              lift match {
                case ScalarQueryLift(name, batch: Tree, encoder: Tree) =>
                  ScalarValueLift("value", value, encoder)
                case CaseClassQueryLift(name, batch: Tree) =>
                  CaseClassValueLift("value", value)
              }
            val (ast, _) = reifyLiftings(BetaReduction(body, alias -> nestedLift))
            c.untypecheck {
              call(batch, param, expand(ast))
            }
        }
      case other =>
        c.fail(s"Batch actions must be static quotations. Found: '$other'")
    }

  private def returningColumn =
    q"""
      expanded.ast match {
        case io.getquill.ast.Returning(_, _, io.getquill.ast.Property(_, property)) => 
          expanded.naming.column(property)
        case ast => 
          io.getquill.util.Messages.fail(s"Can't find returning column. Ast: '$$ast'")
      }
    """

  private def returningExtractor[T](implicit t: WeakTypeTag[T]) =
    q"(row: ${c.prefix}.ResultRow) => implicitly[Decoder[$t]].apply(0, row)"
}
