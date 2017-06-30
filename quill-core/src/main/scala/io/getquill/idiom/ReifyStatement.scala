package io.getquill.idiom

import io.getquill.ast._
import io.getquill.util.Interleave
import io.getquill.idiom.StatementInterpolator._

object ReifyStatement {

  def apply(
    liftingPlaceholder:    Int => String,
    emptySetContainsToken: Token => Token,
    statement:             Statement,
    forProbing:            Boolean
  ): (String, List[ScalarLift]) = {
    def apply(acc: (String, List[ScalarLift]), token: Token): (String, List[ScalarLift]) =
      (acc, token) match {
        case ((s1, liftings), StringToken(s2))            => (s"$s1$s2", liftings)
        case ((s1, liftings), Statement(tokens))          => tokens.foldLeft((s1, liftings))(apply)
        case ((s1, liftings), ScalarLiftToken(lift))      => (s"$s1${liftingPlaceholder(liftings.size)}", liftings :+ lift)
        case ((s1, liftings), SetContainsToken(a, op, b)) => apply(s1 -> liftings, stmt"$a $op ($b)")
      }
    val expanded =
      forProbing match {
        case true  => statement
        case false => expandLiftings(statement, emptySetContainsToken)
      }
    apply(("", List.empty), expanded)
  }

  private def expandLiftings(statement: Statement, emptySetContainsToken: Token => Token) = {
    Statement {
      statement.tokens.foldLeft(List.empty[Token]) {
        case (tokens, SetContainsToken(a, op, ScalarLiftToken(lift: ScalarQueryLift))) =>
          lift.value.asInstanceOf[Traversable[Any]].toList match {
            case Nil => tokens :+ emptySetContainsToken(a)
            case values =>
              val liftings = values.map(v => ScalarLiftToken(ScalarValueLift(lift.name, v, lift.encoder)))
              val separators = List.fill(liftings.size - 1)(StringToken(", "))
              (tokens :+ stmt"$a $op (") ++ Interleave(liftings, separators) :+ StringToken(")")
          }
        case (tokens, token) =>
          tokens :+ token
      }
    }
  }
}
