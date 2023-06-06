package io.getquill

import java.util.concurrent.atomic.AtomicInteger
import io.getquill.ast.{Action, Query, _}
import io.getquill.ast
import io.getquill.context.sql.idiom
import io.getquill.context.sql.idiom.SqlIdiom.{InsertUpdateStmt, copyIdiom}
import io.getquill.context.{CanInsertReturningWithMultiValues, CanInsertWithMultiValues, CanReturnClause}
import io.getquill.context.sql.idiom._
import io.getquill.idiom.{ScalarTagToken, Statement, Token, ValuesClauseToken}
import io.getquill.idiom.StatementInterpolator._
import io.getquill.norm.{BetaReduction, ExpandReturning, ProductAggregationToken}
import io.getquill.quat.Quat
import io.getquill.sql.norm.NormalizeFilteredActionAliases
import io.getquill.util.Messages.fail

import scala.annotation.tailrec
import scala.collection.immutable.{ListMap, ListSet, Queue}

trait PostgresDialect
    extends SqlIdiom
    with QuestionMarkBindVariables
    with ConcatSupport
    with OnConflictSupport
    with CanReturnClause
    with CanInsertWithMultiValues
    with CanInsertReturningWithMultiValues {

  override protected def productAggregationToken: ProductAggregationToken = ProductAggregationToken.VariableDotStar

  override def astTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case ListContains(ast, body) => stmt"${body.token} = ANY(${ast.token})"
      case c: OnConflict           => conflictTokenizer.token(c)
      case ast                     => super.astTokenizer.token(ast)
    }

  override implicit def operationTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy
  ): Tokenizer[Operation] =
    Tokenizer[Operation] {
      case UnaryOperation(StringOperator.`toLong`, ast) => stmt"${scopedTokenizer(ast)}::bigint"
      case UnaryOperation(StringOperator.`toInt`, ast)  => stmt"${scopedTokenizer(ast)}::integer"
      case operation                                    => super.operationTokenizer.token(operation)
    }

  private[getquill] val preparedStatementId = new AtomicInteger

  override def prepareForProbing(string: String) = {
    var i = 0
    val query = string.flatMap(x =>
      if (x != '?') s"$x"
      else {
        i += 1
        s"$$$i"
      }
    )
    s"PREPARE p${preparedStatementId.incrementAndGet.toString.token} AS $query"
  }

  private[getquill] case class ReplaceReturningAlias(batchAlias: String) extends StatelessTransformer {
    override def apply(e: ast.Action): ast.Action =
      e match {
        case Returning(action, alias, property) =>
          val newAlias    = alias.copy(name = batchAlias)
          val newProperty = BetaReduction(property, alias -> newAlias)
          Returning(action, newAlias, newProperty)
        case ReturningGenerated(action, alias, property) =>
          val newAlias    = alias.copy(name = batchAlias)
          val newProperty = BetaReduction(property, alias -> newAlias)
          ReturningGenerated(action, newAlias, newProperty)
        case _ => super.apply(e)
      }
  }

  override protected def actionTokenizer(insertEntityTokenizer: Tokenizer[Entity])(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[ast.Action] =
    Tokenizer[ast.Action] {
      // Don't need to check if this is supported, we know it is since it's postgres.
      // Also, only do it for updates, for inserts we don't want the Returning Alias to be the returning-clause otherwise
      // it would be something like
      //   INSERT ... RETURNING {batchAlias}.property
      // which we don't want. We want it to just be:
      //   INSERT ... RETURNING property

      case returning @ ReturningAction(action: ast.Update, alias, prop) if (idiomContext.queryType.isBatch) =>
        val batchAlias =
          idiomContext.queryType.batchAlias.getOrElse {
            throw new IllegalArgumentException(
              s"Batch alias not found in the action: ${idiomContext.queryType} but it is a batch context. This should not be possible."
            )
          }
        val returningNew = ReplaceReturningAlias(batchAlias)(returning).asInstanceOf[ReturningAction]
        stmt"${(action: Ast).token} RETURNING ${tokenizeReturningClause(returningNew, Some(returningNew.alias.name))}"

      case ConcatableBatchUpdate(output) =>
        output

      case other =>
        super.actionTokenizer(insertEntityTokenizer).token(other)
    }

  protected def specialPropertyTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ) =
    Tokenizer.withFallback[Ast](this.astTokenizer(_, strategy, idiomContext)) { case p: Property =>
      this.propertyTokenizer.token(p)
    }

  object ConcatableBatchUpdate {

    private[getquill] def columnsAndValuesTogether(
      assignments: List[Assignment]
    )(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy, idiomContext: IdiomContext) =
      assignments.map(assignment =>
        assignment.property match {
          case Property.Opinionated(_, key, renameable, visibility) =>
            (
              tokenizeColumn(strategy, key, renameable).token,
              specialPropertyTokenizer.token(assignment.value)
            )
          case _ => fail(s"Invalid assignment value of ${assignment}. Must be a Property object.")
        }
      )

    // case class UpdateWithValues(action: Statement, where: Statement)
    def unapply(action: ast.Update)(implicit
      actionAstTokenizer: Tokenizer[Ast],
      strategy: NamingStrategy,
      idiomContext: IdiomContext
    ): Option[Statement] =
      //    Typical Postgres batch update syntax
      //    UPDATE people AS p SET id = p.id, name = p.name, age = p.age
      //    FROM (values (1, 'Joe', 111), (2, 'Jack', 222))
      //    AS c(id, name, age)
      //    WHERE c.id = p.id

      // Uses the `alias` passed in as `actionAlias` since that is now assigned to the copied SqlIdiom
      (action, idiomContext.queryType) match {
        case (clause @ Update(Filter(table: Entity, origTableAlias, _), _), IdiomContext.QueryType.Batch(batchAlias)) =>
          // Original Query looks like:
          //   liftQuery(people).foreach(ps => query[Person].filter(p => p.id == ps.id).update(_.name -> ps.name))
          // This has already been transpiled to (foreach part has been removed):
          //   query[Person].filter(p => p.id == STag(A)).update(_.name -> STag(B))
          // SQL Needs to look like:
          //   UPDATE person AS p SET name = ps.name FROM (VALUES ('Joe', 123)) AS ps(name, id) WHERE ps.id = p.id
          // I.e.
          //   UPDATE person AS p SET name = ps.name FROM (VALUES (STag(B), STag(A))) AS ps(name, id) WHERE ps.id = p.id
          // Conceptually, that means the query needs to look like:
          //   query[Person].filter(p => p.id == ps.id).update(_.name -> ps.id) with VALUES (STag(B), STag(A))
          // We don't actually change it to this, we yield the SQL directly but it is a good conceptual model

          // Let's consider this odd case for all examples. There could have the same id-column name in multiple places.
          // (NOTE: STag := ScalarTag, the UUIDs are random so I am just assigning numbers to them for reference. Also when the query is tokenize then turn into `?`)
          // (Also [stuff] is short for List(stuff) syntax)
          // Need to work around how that happens
          //   liftQuery(people).foreach(ps => query[Person].filter(p => p.id == ps.id).update(_.name -> ps.name, _.id -> ps.id)
          // This has already been transpiled to (foreach part has been removed):
          //   query[Person].filter(p => p.id == STag(uid:3)).update(_.name -> STag(uid:1), _.id -> STag(uid:2))
          // For now, blindly shove the name into the aliases section and dedupe
          //   UPDATE person AS p SET name = ps.name, id = ps.id FROM (VALUES ('Joe', 123, 123)) AS ps(name, id, id1) WHERE ps.id = p.id1
          // This should actually be
          //   UPDATE person AS p SET name = ps.name, id = ps.id FROM (VALUES (STag(uid:1), STag(uid:2), STag(uid:3))) AS ps(name, id, id1) WHERE ps.id = p.id1
          // (note `ps` is the batchAlias var)

          // replacedWhere:
          //   All the lifts in the WHERE clause that we need to put into the actual VALUES clause instead
          //   Originally was `WHERE ps.id = STag(uid:3)`
          //   (replacedWhere: `WHERE ps.id = p.id1`, additionalColumns: [id] /*and any other column names of STags in WHERE*/, additionalLifts: [STag(uid:3)])
          val (Update(Filter(table: Entity, tableAlias, replacedWhere), assignments), valuesColumns, valuesLifts) =
            ReplaceLiftings.of(clause)(batchAlias, List())
          if (valuesLifts.nonEmpty) {
            // The SET columns/values i.e. ([name, id], [STag(uid:1), STag(uid:2)]
            val columnsAndValues = columnsAndValuesTogether(assignments)
            // the `ps`
            val colsId = batchAlias
            // The columns that go in the SET clause i.e. `SET name = ps.name, id = ps.id`
            val setColumns = columnsAndValues.map { case (column, value) => stmt"$column = ${value}" }.mkStmt(", ")
            // The columns that go inside ps(name, id, id1) i.e. stmt"name, id, id1"
            val asColumns = valuesColumns.toList.mkStmt(", ")
            val output =
              stmt"UPDATE ${table.token} AS ${tableAlias.token} SET $setColumns FROM (VALUES ${ValuesClauseToken(
                  stmt"(${valuesLifts.toList.map(v => v: External).mkStmt(", ")})"
                )}) AS ${colsId.token}($asColumns) WHERE ${specialPropertyTokenizer.token(replacedWhere)}"
            Some(output)
          } else None

        case (clause @ Update(_: Entity, _), IdiomContext.QueryType.Batch(batchAlias)) =>
          val (Update(table: Entity, assignments), valuesColumns, valuesLifts) =
            ReplaceLiftings.of(clause)(batchAlias, List())

          // Choose table alias based on how assignments clauses were realized. Batch-Alias should mean the same thing as when NormalizeFilteredActionAliases was run in Idiom should the
          // value should be the same thing as the clauses that were realiased.
          if (valuesLifts.nonEmpty) {
            val tableAlias       = NormalizeFilteredActionAliases.chooseAlias(table.name, Some(batchAlias))
            val colsId           = batchAlias
            val columnsAndValues = columnsAndValuesTogether(assignments)
            val setColumns       = columnsAndValues.map { case (column, value) => stmt"$column = ${value}" }.mkStmt(", ")
            val asColumns        = valuesColumns.toList.mkStmt(", ")
            val output =
              stmt"UPDATE ${table.token} AS ${tableAlias.token} SET $setColumns FROM (VALUES ${ValuesClauseToken(
                  stmt"(${valuesLifts.toList.map(v => v: External).mkStmt(", ")})"
                )}) AS ${colsId.token}($asColumns)"
            Some(output)
          } else None

        case _ =>
          None
      }
  }
}

object PostgresDialect extends PostgresDialect

case class ReplaceAssignmentAliases(newAlias: Ident) extends StatelessTransformer {
  override def apply(e: Assignment): Assignment =
    Assignment(newAlias, BetaReduction(e.property, e.alias -> newAlias), BetaReduction(e.value, e.alias -> newAlias))
}

case class ReplaceLiftings(
  foreachIdentName: String,
  existingColumnNames: List[String],
  state: ListMap[String, ScalarTag]
) extends StatefulTransformer[ListMap[String, ScalarTag]] {

  private def columnExists(col: String) =
    existingColumnNames.contains(col) || state.keySet.contains(col)

  def freshIdent(newCol: String) = {
    @tailrec
    def loop(id: String, n: Int): String = {
      val fresh = s"${id}${n}"
      if (!columnExists(fresh))
        fresh
      else
        loop(id, n + 1)
    }
    if (!columnExists(newCol))
      newCol
    else
      loop(newCol, 1)
  }

  private def parseName(name: String) =
    name.replace(".", "_")

  override def apply(e: Ast): (Ast, StatefulTransformer[ListMap[String, ScalarTag]]) =
    e match {
      case lift @ ScalarTag(_, External.Source.UnparsedProperty(propNameRaw)) =>
        val id       = Ident(foreachIdentName, lift.quat)
        val propName = freshIdent(propNameRaw)
        (
          Property.Opinionated(id, propName, Renameable.Fixed, Visibility.neutral),
          ReplaceLiftings(foreachIdentName, existingColumnNames, state + (propName -> lift))
        )
      case _ => super.apply(e)
    }
}
object ReplaceLiftings {
  def of(ast: Ast)(foreachIdent: String, existingColumnNames: List[String]) = {
    val (newAst, transform) = new ReplaceLiftings(foreachIdent, existingColumnNames, ListMap()).apply(ast)
    (newAst, transform.state.map(_._1), transform.state.map(_._2))
  }
}
