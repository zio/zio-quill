package io.getquill.idiom

import io.getquill.ast._
import io.getquill.util.Interleave
import io.getquill.idiom.StatementInterpolator._

import scala.annotation.tailrec

object ReifyStatement {

  def apply(
    liftingPlaceholder:    Int => String,
    emptySetContainsToken: Token => Token,
    statement:             Statement,
    forProbing:            Boolean,
    numRowsOfBatch:        Int = 1
  ): (String, List[External]) = {
    val expanded =
      forProbing match {
        case true  => statement
        case false => expandLiftings(statement, emptySetContainsToken, numRowsOfBatch)
      }
    token2string(expanded, liftingPlaceholder)
  }

  private def token2string(token: Token, liftingPlaceholder: Int => String): (String, List[External]) = {
    @tailrec
    def apply(
      workList:      List[Token],
      sqlResult:     Seq[String],
      liftingResult: Seq[External],
      liftingSize:   Int
    ): (String, List[External]) = workList match {
      case Nil => sqlResult.reverse.mkString("") -> liftingResult.reverse.toList
      case head :: tail =>
        head match {
          case StringToken(s2)            => apply(tail, s2 +: sqlResult, liftingResult, liftingSize)
          case SetContainsToken(a, op, b) => apply(stmt"$a $op ($b)" +: tail, sqlResult, liftingResult, liftingSize)
          case ScalarLiftToken(lift)      => apply(tail, liftingPlaceholder(liftingSize) +: sqlResult, lift +: liftingResult, liftingSize + 1)
          case ScalarTagToken(tag)        => apply(tail, liftingPlaceholder(liftingSize) +: sqlResult, tag +: liftingResult, liftingSize + 1)
          case Statement(tokens)          => apply(tokens.foldRight(tail)(_ +: _), sqlResult, liftingResult, liftingSize)
          case ValuesClauseToken(stmt)    => apply(stmt +: tail, sqlResult, liftingResult, liftingSize)
          case _: QuotationTagToken =>
            throw new UnsupportedOperationException("Quotation Tags must be resolved before a reification.")
        }
    }

    apply(List(token), Seq(), Seq(), 0)
  }

  private def expandLiftings(statement: Statement, emptySetContainsToken: Token => Token, numRowsOfBatch: Int) = {

    Statement {
      statement.tokens.foldLeft(List.empty[Token]) {
        case (tokens, ValuesClauseToken(insertColumns)) =>
          val duplicatedStatements = (0 until numRowsOfBatch).map(_ => insertColumns)
          val separators = List.fill(duplicatedStatements.size - 1)(StringToken(", "))
          Interleave(duplicatedStatements.toList, separators)
        case (tokens, SetContainsToken(a, op, ScalarLiftToken(lift: ScalarQueryLift))) =>
          lift.value.asInstanceOf[Iterable[Any]].toList match {
            case Nil => tokens :+ emptySetContainsToken(a)
            case values =>
              val liftings = values.map(v => ScalarLiftToken(ScalarValueLift(lift.name, v, lift.encoder, lift.quat)))
              val separators = List.fill(liftings.size - 1)(StringToken(", "))
              (tokens :+ stmt"$a $op (") ++ Interleave(liftings, separators) :+ StringToken(")")
          }
        case (tokens, token) =>
          tokens :+ token
      }
    }
  }
}
