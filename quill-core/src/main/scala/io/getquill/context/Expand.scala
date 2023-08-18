package io.getquill.context

import io.getquill.ast._
import io.getquill.NamingStrategy
import io.getquill.idiom._
import io.getquill.IdiomContext
import io.getquill.quat.Quat

object CanDoBatchedInsert {
  def apply(ast: Ast, idiom: Idiom, statement: Token, isReturning: Boolean, idiomContext: IdiomContext): Boolean = {
    // find any actions that could have a VALUES clause. Right now just ast.Insert,
    // in the future might be Update and Delete
    val actions = CollectAst.byType[Action](ast)
    // only one action allowed per-query in general
    if (actions.length != 1)
      false
    else {
      val validations =
        for {
          _ <- validateConcatenatedIterationPossible(statement).right
          _ <- validateIdiomSupportsConcatenatedIteration(idiom, isReturning).right
        } yield ()

      validations match {
        case Right(_)  => true
        case Left(msg) => false
      }
    }
  }

  private def validateIdiomSupportsConcatenatedIteration(idiom: Idiom, doingReturning: Boolean): Either[String, Unit] =
    doingReturning match {
      case false =>
        validateIdiomSupportsConcatenatedIterationNormal(idiom)
      case true =>
        validateIdiomSupportsConcatenatedIterationReturning(idiom)
    }

  private def validateIdiomSupportsConcatenatedIterationNormal(idiom: Idiom): Either[String, Unit] = {
    val hasCapability =
      if (idiom.isInstanceOf[IdiomInsertValueCapability])
        idiom.asInstanceOf[IdiomInsertValueCapability].idiomInsertValuesCapability == InsertValueMulti
      else
        false

    if (hasCapability)
      Right(())
    else
      Left(
        s"""|The dialect ${idiom.getClass.getName} does not support inserting multiple rows-per-batch (e.g. it cannot support multiple VALUES clauses).
            |Currently this functionality is only supported for INSERT queries for select databases (Postgres, H2, SQL Server, Sqlite).
            |Falling back to the regular single-row-per-batch insert behavior.
            |""".stripMargin
      )
  }

  private def validateIdiomSupportsConcatenatedIterationReturning(idiom: Idiom): Either[String, Unit] = {
    val hasCapability =
      if (idiom.isInstanceOf[IdiomInsertReturningValueCapability])
        idiom
          .asInstanceOf[IdiomInsertReturningValueCapability]
          .idiomInsertReturningValuesCapability == InsertReturningValueMulti
      else
        false

    if (hasCapability)
      Right(())
    else
      Left(
        s"""|The dialect ${idiom.getClass.getName} does not support inserting multiple rows-per-batch (e.g. it cannot support multiple VALUES clauses)
            |when batching with query-returns and/or generated-keys.
            |Currently this functionality is only supported for INSERT queries for select databases (Postgres, H2, SQL Server).
            |Falling back to the regular single-row-per-batch insert-returning behavior.
            |""".stripMargin
      )
  }

  private def validateConcatenatedIterationPossible(realQuery: Token): Either[String, Unit] = {
    import io.getquill.idiom._
    def valueClauseExistsIn(token: Token): Boolean =
      token match {
        case _: ValuesClauseToken           => true
        case _: StringToken                 => false
        case _: ScalarTagToken              => false
        case _: QuotationTagToken           => false
        case _: ScalarLiftToken             => false
        case Statement(tokens: List[Token]) => tokens.exists(valueClauseExistsIn(_) == true)
        case SetContainsToken(a: Token, op: Token, b: Token) =>
          valueClauseExistsIn(a) || valueClauseExistsIn(op) || valueClauseExistsIn(b)
      }

    if (valueClauseExistsIn(realQuery))
      Right(())
    else
      Left(
        s"""|Cannot insert multiple rows per-batch-query since the query has no VALUES clause.
            |Currently this functionality is only supported for INSERT queries for select databases (Postgres, H2, SQL Server, Sqlite).
            |Falling back to the regular single-row-per-batch insert behavior.
            |""".stripMargin
      )
  }
}

case class Expand[C <: Context[_, _]](
  val context: C,
  val ast: Ast,
  statement: Statement,
  idiom: Idiom,
  naming: NamingStrategy,
  executionType: ExecutionType
) {

  val (string, externals) =
    ReifyStatement(
      idiom.liftingPlaceholder,
      idiom.emptySetContainsToken,
      statement,
      forProbing = false
    )

  val liftings = externals.collect { case lift: ScalarLift =>
    lift
  }

  val prepare =
    (row: context.PrepareRow, session: context.Session) => {
      val (_, values, prepare) = liftings.foldLeft((0, List.empty[Any], row)) { case ((idx, values, row), lift) =>
        val encoder = lift.encoder.asInstanceOf[context.Encoder[Any]]
        val newRow  = encoder(idx, lift.value, row, session)
        (idx + 1, lift.value :: values, newRow)
      }
      (values, prepare)
    }
}

case class ExpandWithInjectables[T, C <: Context[_, _]](
  val context: C,
  val ast: Ast,
  statement: Statement,
  idiom: Idiom,
  naming: NamingStrategy,
  executionType: ExecutionType,
  subBatch: List[T],
  injectables: List[(String, T => ScalarLift)]
) {

  val (string, externals) =
    ReifyStatementWithInjectables(
      idiom.liftingPlaceholder,
      idiom.emptySetContainsToken,
      statement,
      forProbing = false,
      subBatch,
      injectables
    )

  val liftings = externals.collect { case lift: ScalarLift =>
    lift
  }

  val prepare =
    (row: context.PrepareRow, session: context.Session) => {
      val (_, values, prepare) = liftings.foldLeft((0, List.empty[Any], row)) { case ((idx, values, row), lift) =>
        val encoder = lift.encoder.asInstanceOf[context.Encoder[Any]]
        val newRow  = encoder(idx, lift.value, row, session)
        (idx + 1, lift.value :: values, newRow)
      }
      (values, prepare)
    }
}
