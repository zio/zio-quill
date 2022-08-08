package io.getquill.context

import io.getquill.ast._
import io.getquill.idiom.{ Idiom, ReifyStatement, Statement }
import io.getquill.NamingStrategy

object ValueClauseExistsIn {
  import io.getquill.idiom._
  def apply(token: Token): Boolean =
    token match {
      case _: ValuesClauseToken           => true
      case _: StringToken                 => false
      case _: ScalarTagToken              => false
      case _: QuotationTagToken           => false
      case _: ScalarLiftToken             => false
      case Statement(tokens: List[Token]) => tokens.exists(apply(_) == true)
      case SetContainsToken(a: Token, op: Token, b: Token) =>
        apply(a) || apply(op) || apply(b)
    }
}

case class Expand[C <: Context[_, _]](
  val context:   C,
  val ast:       Ast,
  statement:     Statement,
  idiom:         Idiom,
  naming:        NamingStrategy,
  executionType: ExecutionType,
  numRowsOfBatch:       Int = 1
) {

  val (string, externals) =
    ReifyStatement(
      idiom.liftingPlaceholder,
      idiom.emptySetContainsToken,
      statement,
      forProbing = false,
      numRowsOfBatch = numRowsOfBatch
    )

  val liftings = externals.collect {
    case lift: ScalarLift => lift
  }

  val prepare =
    (row: context.PrepareRow, session: context.Session) => {
      val (_, values, prepare) = liftings.foldLeft((0, List.empty[Any], row)) {
        case ((idx, values, row), lift) =>
          val encoder = lift.encoder.asInstanceOf[context.Encoder[Any]]
          val newRow = encoder(idx, lift.value, row, session)
          (idx + 1, lift.value :: values, newRow)
      }
      (values, prepare)
    }
}
