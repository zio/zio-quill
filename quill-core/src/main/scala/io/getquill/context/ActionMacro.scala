package io.getquill.context

import io.getquill.ast._ // Only .returning(r => r.prop) or .returning(r => OneElementCaseClass(r.prop)) is allowed.
import io.getquill.norm.BetaReduction
import io.getquill.quotation.ReifyLiftings
import io.getquill.util.MacroContextExt._

import scala.reflect.macros.whitebox.{ Context => MacroContext }
import io.getquill.util.{ EnableReflectiveCalls, OptionalTypecheck }

class ActionMacro(val c: MacroContext)
  extends ContextMacro
  with ReifyLiftings {

  import c.universe.{ Function => _, Ident => _, _ }

  def translateQuery(quoted: Tree): Tree =
    translateQueryPrettyPrint(quoted, q"false")

  def translateQueryPrettyPrint(quoted: Tree, prettyPrint: Tree): Tree =
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val expanded = ${expand(extractAst(quoted))}
        ${c.prefix}.translateQuery(
          expanded.string,
          expanded.prepare,
          prettyPrint = ${prettyPrint}
        )
      """
    }

  def translateBatchQuery(quoted: Tree): Tree =
    translateBatchQueryPrettyPrint(quoted, q"false")

  def translateBatchQueryPrettyPrint(quoted: Tree, prettyPrint: Tree): Tree =
    expandBatchAction(quoted) {
      case (batch, param, expanded) =>
        q"""
          ..${EnableReflectiveCalls(c)}
          ${c.prefix}.translateBatchQuery(
            $batch.map { $param =>
              val expanded = $expanded
              (expanded.string, expanded.prepare)
            }.groupBy(_._1).map {
              case (string, items) =>
                ${c.prefix}.BatchGroup(string, items.map(_._2).toList)
            }.toList,
            ${prettyPrint}
          )
        """
    }

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
    batchAction(quoted, "executeBatchAction")

  def prepareBatchAction(quoted: Tree): Tree =
    batchAction(quoted, "prepareBatchAction")

  def batchAction(quoted: Tree, method: String): Tree =
    expandBatchAction(quoted) {
      case (batch, param, expanded) =>
        q"""
          ..${EnableReflectiveCalls(c)}
          ${c.prefix}.${TermName(method)}(
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
                case ScalarQueryLift(name, batch: Tree, encoder: Tree, quat) =>
                  ScalarValueLift("value", value, encoder, quat)
                case CaseClassQueryLift(name, batch: Tree, quat) =>
                  CaseClassValueLift("value", value, quat)
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
      (expanded.ast match {
        case ret: io.getquill.ast.ReturningAction =>
            io.getquill.norm.ExpandReturning.applyMap(ret)(
              (ast, statement) => io.getquill.context.Expand(${c.prefix}, ast, statement, idiom, naming).string
            )(idiom, naming)
        case ast =>
          io.getquill.util.Messages.fail(s"Can't find returning column. Ast: '$$ast'")
      })
    """

  def prepareAction(quoted: Tree): Tree =
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val expanded = ${expand(extractAst(quoted))}
        ${c.prefix}.prepareAction(
          expanded.string,
          expanded.prepare
        )
      """
    }

  private def returningExtractor[T](implicit t: WeakTypeTag[T]) = {
    OptionalTypecheck(c)(q"implicitly[${c.prefix}.Decoder[$t]]") match {
      case Some(decoder) =>
        q"(row: ${c.prefix}.ResultRow) => $decoder.apply(0, row)"
      case None =>
        val metaTpe = c.typecheck(tq"${c.prefix}.QueryMeta[$t]", c.TYPEmode).tpe
        val meta = c.inferImplicitValue(metaTpe).orElse(q"${c.prefix}.materializeQueryMeta[$t]")
        q"$meta.extract"
    }
  }
}
