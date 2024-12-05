package io.getquill.context

import io.getquill.ast._
import io.getquill.norm.BetaReduction
import io.getquill.quat.Quat
import io.getquill.quotation.ReifyLiftings
import io.getquill.util.MacroContextExt._

import scala.reflect.macros.whitebox.{Context => MacroContext}
import io.getquill.util.{EnableReflectiveCalls, OptionalTypecheck}

import java.util.UUID

class ActionMacro(val c: MacroContext) extends ContextMacro with ReifyLiftings {

  import c.universe.{Function => _, Ident => _, _}

  def translateQuery(quoted: Tree): Tree =
    translateQueryPrettyPrint(quoted, q"io.getquill.context.TranslateOptions()")

  def translateQueryPrettyPrint(quoted: Tree, options: Tree): Tree = {
    val expanded = expand(extractAst(quoted), inferQuat(quoted.tpe))
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val (idiomContext, expanded) = $expanded
        ${c.prefix}.translateQuery(
          expanded.string,
          expanded.liftings,
          options = ${options}
        )(io.getquill.context.ExecutionInfo.unknown, ())
      """
    }
  }

  def translateBatchQuery(quoted: Tree): Tree =
    translateBatchQueryPrettyPrint(quoted, q"io.getquill.context.TranslateOptions()")

  // TODO need to change this to include liftings
  def translateBatchQueryPrettyPrint(quoted: Tree, options: Tree): Tree =
    expandBatchActionNew(quoted, isReturning = false) {
      case (batch, param, expanded, injectableLiftList, idiomNamingOriginalAstVars, idiomContext, canDoBatch) =>
        q"""
          ..${EnableReflectiveCalls(c)}
          val batches =
            if ($canDoBatch) {
              $batch.toList.grouped(1).toList
            } else {
              $batch.toList.map(element => List(element))
            }
          ${c.prefix}.translateBatchQuery(
            batches.map { subBatch =>
              val expanded = $expanded
              (expanded.string, expanded.prepare, expanded.liftings)
            }.groupBy(_._1).map {
              case (string, items) =>
                ${c.prefix}.BatchGroup(string, items.map(_._2).toList, items.map(_._3).toList)
            }.toList,
            $options
          )(io.getquill.context.ExecutionInfo.unknown, ())
        """
    }

  def runAction(quoted: Tree): Tree = {
    val expanded = expand(extractAst(quoted), Quat.Value)
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val (idiomContext, expanded) = $expanded
        ${c.prefix}.executeAction(
          expanded.string,
          expanded.prepare
        )(io.getquill.context.ExecutionInfo.unknown, ())
      """
    }
  }

  def runActionReturning[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree = {
    val expanded = expand(extractAst(quoted), inferQuat(t.tpe))
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val (idiomContext, expanded) = $expanded
        ${c.prefix}.executeActionReturning(
          expanded.string,
          expanded.prepare,
          ${returningExtractor[T]},
          $returningColumn
        )(io.getquill.context.ExecutionInfo.unknown, ())
      """
    }
  }

  def runActionReturningMany[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree = {
    val expanded = expand(extractAst(quoted), inferQuat(t.tpe))
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val (idiomContext, expanded) = $expanded
        ${c.prefix}.executeActionReturningMany(
          expanded.string,
          expanded.prepare,
          ${returningExtractor[T]},
          $returningColumn
        )(io.getquill.context.ExecutionInfo.unknown, ())
      """
    }
  }

  // Called from: run(BatchAction)
  def runBatchAction(quoted: Tree): Tree = batchAction(quoted, "executeBatchAction")
  // Called from: run(BatchAction, 10)
  def runBatchActionRows(quoted: Tree, numRows: Tree): Tree = batchActionRows(quoted, "executeBatchAction", numRows)

  def prepareBatchAction(quoted: Tree): Tree =
    batchAction(quoted, "prepareBatchAction")

  def batchAction(quoted: Tree, method: String): Tree =
    // In the future, default num rows should be injected from a Messages variable
    batchActionRows(quoted, method, q"1")

  def batchActionRows(quoted: Tree, method: String, numRows: Tree): Tree =
    expandBatchActionNew(quoted, isReturning = false) {
      case (batch, param, expanded, injectableLiftList, idiomNamingOriginalAstVars, idiomContext, canDoBatch) =>
        q"""
          ..${EnableReflectiveCalls(c)}
          ${c.prefix}.${TermName(method)}({
            /*
            Using this to preserver order of inserts generated from the tokenization using the groupByOrdered below.
            Practically this means that we want the encountered insert queries `INSERT (...) VALUES (x,y,z),(x,y,z)... INSERT (...) VALUES (x,y,z)`
            to stay in the same order
            */
            import io.getquill.util.OrderedGroupByExt._
            val originalAst = $idiomNamingOriginalAstVars
            val idiomContext = $idiomContext
            /* for liftQuery(people:List[Person]) `batch` is `people` */
            /* TODO Need secondary check to see if context is actually capable of batch-values insert */
            /* If there is a INSERT ... VALUES clause this will be encoded as ValuesClauseToken(lifts) which we need to duplicate */
            /* batches: List[List[Person]] */
            val batches =
              if ($canDoBatch && $numRows != 1) {
                $batch.toList.grouped($numRows).toList
              } else {
                $batch.toList.map(element => List(element))
              }
            /* batchesSharded: List[(String, (Row, MirrorSession) => (List[Any], Row <a.k.a: prepare>), List[ScalarLift]) ] */
            val batchesSharded = batches.map { subBatch => {
                /* `expanded` is io.getquill.context.ExpandWithInjectables(ast, subBatch, injectableLiftList) */
                val expanded = $expanded
                (expanded.string, expanded.prepare, expanded.liftings)
              }
            }
            /*
            So when you have:
            List(joe, jack, jim, jill, caboose)
            it will expand to batchesSharded:
            So
              (INSERT ... VALUES (? ?), (?, ?), List(joe, jack)),
              (INSERT ... VALUES (? ?), (?, ?), List(jim, jill)),
              (INSERT ... VALUES (? ?),         List(caboose))
            ...but then will be grouped into (using the Query-string):
              (INSERT ... VALUES (? ?), (?, ?), List(List(joe, jack), List(jim, jill))),
              (INSERT ... VALUES (? ?),         List(caboose))
            */
            batchesSharded.groupByOrdered(_._1).map {
              case (string, items) =>
                ${c.prefix}.BatchGroup(string, items.map(_._2).toList, items.map(_._3).toList)
            }.toList
          })(io.getquill.context.ExecutionInfo.unknown, ())
        """
    }

  // Called from: run(BatchAction)
  def runBatchActionReturning[T](quoted: Tree)(implicit t: WeakTypeTag[T]): Tree =
    batchActionReturningRows(quoted, q"1")
  // Called from: run(BatchAction, 10)
  def runBatchActionReturningRows[T](quoted: Tree, numRows: Tree)(implicit t: WeakTypeTag[T]): Tree =
    batchActionReturningRows(quoted, numRows)

  def batchActionReturningRows[T](quoted: Tree, numRows: Tree)(implicit t: WeakTypeTag[T]): Tree =
    expandBatchActionNew(quoted, isReturning = true) {
      case (batch, param, expanded, injectableLiftList, idiomNamingOriginalAstVars, idiomContext, canDoBatch) =>
        q"""
          ..${EnableReflectiveCalls(c)}
          ${c.prefix}.executeBatchActionReturning({
            import io.getquill.util.OrderedGroupByExt._
            val originalAst = $idiomNamingOriginalAstVars
            val idiomContext = $idiomContext
            val batches =
              if ($canDoBatch && $numRows != 1) {
                $batch.toList.grouped($numRows).toList
              } else {
                $batch.toList.map(element => List(element))
              }
            val batchesSharded = batches.map { subBatch => {
                /* `expanded` is io.getquill.context.ExpandWithInjectables(ast, subBatch, injectableLiftList) */
                val expanded = $expanded
                ((expanded.string, $returningColumn), expanded.prepare, expanded.liftings)
              }
            }
            batchesSharded.groupByOrdered(_._1).map {
              case ((string, column), items) =>
                ${c.prefix}.BatchGroupReturning(string, column, items.map(_._2).toList, items.map(_._3).toList)
            }.toList
          }, ${returningExtractor[T]})(io.getquill.context.ExecutionInfo.unknown, ())
        """
    }

  def expandBatchActionNew(quoted: Tree, isReturning: Boolean)(
    call: (Tree, Tree, Tree, Tree, Tree, Tree, Tree) => Tree
  ): Tree =
    BetaReduction(extractAst(quoted)) match {
      case totalAst @ Foreach(lift: Lift, alias, body) =>
        // for liftQuery(people:List[Person]) this is: `people`
        val batch = lift.value.asInstanceOf[Tree]
        // This would be the Type[Person]
        val batchItemType = batch.tpe.typeArgs.head
        // So we type-check (value: Person) => value
        c.typecheck(q"(value: $batchItemType) => value") match {
          case q"($param) => $values" =>
            // So this becomes: (value: Person) => ScalarValueLift("value", value, Encoder[Person], quatOf[Person])
            //     or possibly: (value: Person) => CaseClassValueLift("value", value, Encoder[Person], quatOf[Person])
            val nestedLift =
              lift match {
                case ScalarQueryLift(name, batch: Tree, encoder: Tree, quat) =>
                  ScalarValueLift("value", External.Source.UnparsedProperty("value"), q"$values", encoder, quat)
                case CaseClassQueryLift(name, batch: Tree, quat) =>
                  CaseClassValueLift("value", "value", q"$values", quat)
              }

            // So then on the AST-level we transform the alias `p` **
            // from this: `foreach(people).map(p => insert(p.name, p.age))`
            // into this: `foreach(people).map(p => insert(CaseClassValue("value", value:Person, encoder[Person], quatOf[Person]).name, CCV(...).age)))
            // ReifyLiftings will then turn it
            // into this: `foreach(people).map(p => insert(CaseClassValue("value", (value:Person).name, encoder[String], quatOf[Person]), CCV(... (value:Person).age ...))))
            //
            // (** Note that I mixing the scala-api way of seeing this DSL i.e. foreach instead of ast.Foreach
            // and the regular one i.e CaseClassValue. That's the only way to see what's going on without information-overload.
            // also CCV:=CaseClassValue)
            //
            // Note that update cases are more complex:
            // from this: `foreach(people).map(p => filter(pp => pp.id == p.id).update(p.name, p.age))`
            // into this: `foreach(people).map(p => filter(pp => pp.id == CCV(value:Person,...)).update(CCV(value:Person,...).name, CCV(...).age)))
            // ReifyLiftings will then turn it
            // into this: `foreach(people).map(p => filter(pp => pp.id == CCV((value:Person).id,...).update(CCV((value:Person).name), CCV(... (value:Person).age ...))))
            // in order to be able to do things like VALUES-clause inserts we need to preserve the original knowledge that the property was `Property(Id(p),"name").
            val (valuePluggingAst, _) = reifyLiftings(BetaReduction(body, alias -> nestedLift))
            // this is the ast with ScalarTag placeholders for the lifts
            val (ast, valuePlugList) = ExtractLiftings.of(valuePluggingAst)
            val liftUnlift           = new { override val mctx: c.type = c } with TokenLift(ast.countQuatFields)
            // List(id1 -> ((p: Person) => CCV(p.name), id2 -> ((p: Person) => CCV(p.age), ...)
            // For regular lifts (e.g. liftQuery(people).foreach(p => query[Person].filter(pp => pp.name == lift("a regular lift").insert(...)))
            // we can just do (p: Person) => "a regular lift" and nothing will be done with `p`
            val injectableLiftListTrees =
              valuePlugList.map { case (id, valuePlugLift) =>
                q"($id, ($param) => ${liftUnlift.astLiftable(valuePlugLift)})"
              }
            val injectableLiftList = q"$injectableLiftListTrees"

            val (translatedAst, token, executionType, idiomContext, string, idiom) =
              tryTranslateStatic(ast, Quat.Unknown, Some(alias.name)).fold(msg => c.fail(msg), x => x)

            // Splice into the code to tokenize the ast (i.e. the Expand class) and compile-time translate the AST if possible
            val expanded = {
              val liftUnlift = new { override val mctx: c.type = c } with TokenLift(ast.countQuatFields)
              import liftUnlift._
              import ConfigLiftables._
              q"""
              val (ast, statement, executionType) = ($translatedAst, $token, ${executionType: ExecutionType})
              io.getquill.context.ExpandWithInjectables(${c.prefix}, ast, statement, idiom, naming, executionType, subBatch, $injectableLiftList)
              """
            }

            val canDoBatch = CanDoBatchedInsert(totalAst, idiom, token, isReturning, idiomContext)

            val idiomNamingOriginalAstVars =
              q"""
              val (idiom, naming) = ${idiomAndNamingDynamic};
              ${liftUnlift.astLiftable.apply((ast))}
              """

            val transpileContextExpr = ConfigLiftables.transpileContextLiftable(idiomContext)
            val canDoBatchExpr       = q"$canDoBatch"
            c.untypecheck {
              call(
                batch,
                param,
                expanded,
                injectableLiftList,
                idiomNamingOriginalAstVars,
                transpileContextExpr,
                canDoBatchExpr
              )
            }
        }
      case other =>
        c.fail(s"""Batch actions must be static quotations. Found: '$other'.
        |It's possible this is being caused by type ascription when declaring the implicit
        |schema meta, so try removing it if that's the case, e.g.:
        `implicit val schema: ctx.SchemaMeta[Row] = schemaMeta[Row]("rows")` ->
        `implicit val schema = schemaMeta[Row]("rows")`""")
    }

  object ExtractLiftings {
    def of(ast: Ast): (Ast, List[(String, ScalarLift)]) = {
      val (outputAst, extracted) = ExtractLiftings(List.empty)(ast)
      (outputAst, extracted.state.map { case (tag, lift) => (tag.uid, lift) })
    }
  }
  case class ExtractLiftings(state: List[(ScalarTag, ScalarLift)])
      extends StatefulTransformer[List[(ScalarTag, ScalarLift)]] {

    override def apply(e: Action): (Action, StatefulTransformer[List[(ScalarTag, ScalarLift)]]) =
      e match {
        case Insert(body, assignments) =>
          val (newAssignments, assignmentMappings) = apply(assignments)(_.apply)
          (Insert(body, newAssignments), assignmentMappings)
        case _ =>
          super.apply(e)
      }

    // Only extract lifts that come from values-clauses:
    // liftQuery(people).foreach(ps => query[Person].filter(_.name == lift("not this")).insertValue(_.name -> <these!>, ...))
    override def apply(e: Ast): (Ast, StatefulTransformer[List[(ScalarTag, ScalarLift)]]) =
      e match {
        case rawLift @ ScalarValueLift(_, rawSource @ External.Source.UnparsedProperty(rawSourceName), _, _, _) =>
          val uuid      = UUID.randomUUID().toString
          val source    = External.Source.UnparsedProperty(rawSourceName.stripPrefix("value.").replace(".", "_"))
          val scalarTag = ScalarTag(uuid, source)
          val lift      = rawLift.copy(source = source)
          (scalarTag, ExtractLiftings((scalarTag -> lift) +: state))
        case _ => super.apply(e)
      }
  }

  private def returningColumn =
    q"""
      (expanded.ast match {
        case ret: io.getquill.ast.ReturningAction =>
            io.getquill.norm.ExpandReturning.applyMap(ret)(
              (ast, statement) => io.getquill.context.Expand(${c.prefix}, ast, statement, idiom, naming, io.getquill.context.ExecutionType.Unknown).string
            )(idiom, naming, idiomContext)
        case ast =>
          io.getquill.util.Messages.fail(s"Can't find returning column. Ast: '$$ast'")
      })
    """

  def prepareAction(quoted: Tree): Tree =
    c.untypecheck {
      q"""
        ..${EnableReflectiveCalls(c)}
        val (idiomContext, expanded) = ${expand(extractAst(quoted), Quat.Value)}
        ${c.prefix}.prepareAction(
          expanded.string,
          expanded.prepare
        )(io.getquill.context.ExecutionInfo.unknown, ())
      """
    }

  private def returningExtractor[T](implicit t: WeakTypeTag[T]) =
    OptionalTypecheck(c)(q"implicitly[${c.prefix}.Decoder[$t]]") match {
      case Some(decoder) =>
        q"(row: ${c.prefix}.ResultRow, session: ${c.prefix}.Session) => $decoder.apply(0, row, session)"
      case None =>
        val metaTpe = c.typecheck(tq"${c.prefix}.QueryMeta[$t]", c.TYPEmode).tpe
        val meta    = c.inferImplicitValue(metaTpe).orElse(q"${c.prefix}.materializeQueryMeta[$t]")
        q"$meta.extract"
    }
}
