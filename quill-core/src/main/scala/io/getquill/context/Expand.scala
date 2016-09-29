package io.getquill.context

import io.getquill.ast._
import io.getquill.idiom.Statement
import io.getquill.idiom.ReifyStatement
import io.getquill.NamingStrategy
import io.getquill.idiom.Idiom

case class Expand[C <: Context[_, _]](
  val context: C,
  val ast:     Ast,
  statement:   Statement,
  idiom:       Idiom,
  naming:      NamingStrategy
) {

  val (string, liftings) =
    ReifyStatement(
      idiom.liftingPlaceholder,
      idiom.emptyQuery,
      statement,
      forProbing = false
    )

  val prepare =
    (row: context.PrepareRow) =>
      (liftings.foldLeft((0, row)) {
        case ((idx, row), lift) =>
          val encoder = lift.encoder.asInstanceOf[context.Encoder[Any]]
          val newRow = encoder(idx, lift.value, row)
          (idx + 1, newRow)
      })._2
}