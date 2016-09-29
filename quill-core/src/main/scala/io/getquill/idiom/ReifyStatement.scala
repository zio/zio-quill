package io.getquill.idiom

import io.getquill.ast._
import io.getquill.util.Interleave

object ReifyStatement {

  def apply(
    liftingPlaceholder: Int => String,
    emptyQuery:         String,
    statement:          Statement,
    forProbing:         Boolean
  ): (String, List[ScalarLift]) = {
    def apply(acc: (String, List[ScalarLift]), token: Token): (String, List[ScalarLift]) =
      (acc, token) match {
        case ((s1, liftings), StringToken(s2))       => (s"$s1$s2", liftings)
        case ((s1, liftings), Statement(tokens))     => tokens.foldLeft((s1, liftings))(apply)
        case ((s1, liftings), ScalarLiftToken(lift)) => (s"$s1${liftingPlaceholder(liftings.size)}", liftings :+ lift)
      }
    val expanded =
      forProbing match {
        case true  => statement
        case false => expandLiftings(statement, emptyQuery)
      }
    apply(("", List.empty), expanded)
  }

  private def expandLiftings(statement: Statement, emptyQuery: String) = {
    Statement {
      statement.tokens.foldLeft(List.empty[Token]) {
        case (tokens, ScalarLiftToken(lift: ScalarQueryLift)) =>
          lift.value.asInstanceOf[Traversable[Any]].toList match {
            case Nil => tokens :+ StringToken(emptyQuery)
            case values =>
              val liftings = values.map(v => ScalarLiftToken(ScalarValueLift(lift.name, v, lift.encoder)))
              val separators = List.fill(liftings.size - 1)(StringToken(", "))
              tokens ++ Interleave(liftings, separators)
          }
        case (tokens, token) =>
          tokens :+ token
      }
    }
  }
}
