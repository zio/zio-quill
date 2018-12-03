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
      idiom.emptySetContainsToken,
      statement,
      forProbing = false
    )

  val prepare =
    (row: context.PrepareRow) => {
      val (_, values, prepare) = liftings.foldLeft((0, List.empty[Any], row)) {
        case ((idx, values, row), lift) =>
          val encoder = lift.encoder.asInstanceOf[context.Encoder[Any]]
          val newRow = encoder(idx, lift.value, row)
          (idx + 1, lift.value :: values, newRow)
      }
      (values, prepare)
    }
}
