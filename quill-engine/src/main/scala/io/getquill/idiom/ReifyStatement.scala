package io.getquill.idiom

import io.getquill.ast._
import io.getquill.idiom.StatementInterpolator._
import io.getquill.util.Interleave

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

object ReifyStatement {

  def apply(
    liftingPlaceholder: Int => String,
    emptySetContainsToken: Token => Token,
    statement: Statement,
    forProbing: Boolean
  ): (String, List[External]) = {
    val expanded =
      if (forProbing) statement
      else expandLiftings(statement, emptySetContainsToken)

    token2string(expanded, liftingPlaceholder)
  }

  private def token2string(token: Token, liftingPlaceholder: Int => String): (String, List[External]) = {
    @tailrec
    def apply(
      workList: List[Token],
      sqlResult: ListBuffer[String],
      liftingResult: ListBuffer[External],
      liftingSize: Int
    ): (String, List[External]) =
      workList match {
        case Nil => sqlResult.mkString("") -> liftingResult.toList
        case head :: tail =>
          head match {
            case StringToken(s2)            => apply(tail, sqlResult += s2, liftingResult, liftingSize)
            case SetContainsToken(a, op, b) => apply(stmt"$a $op ($b)" +: tail, sqlResult, liftingResult, liftingSize)
            case ScalarLiftToken(lift) =>
              apply(tail, sqlResult += liftingPlaceholder(liftingSize), liftingResult += lift, liftingSize + 1)
            case ScalarTagToken(tag) =>
              apply(tail, sqlResult += liftingPlaceholder(liftingSize), liftingResult += tag, liftingSize + 1)
            case Statement(tokens)       => apply(tokens.foldRight(tail)(_ +: _), sqlResult, liftingResult, liftingSize)
            case ValuesClauseToken(stmt) => apply(stmt +: tail, sqlResult, liftingResult, liftingSize)
            case _: QuotationTagToken =>
              throw new UnsupportedOperationException("Quotation Tags must be resolved before a reification.")
          }
      }

    apply(List(token), ListBuffer.empty, ListBuffer.empty, 0)
  }

  private val `, ` : StringToken = StringToken(", ")
  private val `)` : StringToken  = StringToken(")")

  private def expandLiftings(statement: Statement, emptySetContainsToken: Token => Token): Statement =
    Statement {
      statement.tokens
        .foldLeft(List.empty[Token]) {
          case (tokens, SetContainsToken(a, op, ScalarLiftToken(lift: ScalarQueryLift))) =>
            val iterable = lift.value.asInstanceOf[Iterable[Any]]
            if (iterable.isEmpty) tokens :+ emptySetContainsToken(a)
            else {
              val liftings = iterable.map(v =>
                ScalarLiftToken(
                  ScalarValueLift(lift.name, External.Source.Parser, v, lift.encoder, lift.quat)
                )
              )
              val separators = List.fill(liftings.size - 1)(`, `)
              (tokens :+ stmt"$a $op (") ++ Interleave(liftings.toList, separators) :+ `)`
            }
          case (tokens, token) =>
            tokens :+ token
        }
    }
}

object ReifyStatementWithInjectables {

  def apply[T](
    liftingPlaceholder: Int => String,
    emptySetContainsToken: Token => Token,
    statement: Statement,
    forProbing: Boolean,
    subBatch: List[T],
    injectables: List[(String, T => ScalarLift)]
  ): (String, List[External]) = {
    val expanded =
      if (forProbing) statement
      else expandLiftings(statement, emptySetContainsToken, subBatch, injectables.toMap)

    val (query, externals) = token2string(expanded, liftingPlaceholder)
    (query, externals)
  }

  private def token2string(token: Token, liftingPlaceholder: Int => String): (String, List[External]) = {
    @tailrec
    def apply(
      workList: List[Token],
      sqlResult: Seq[String],
      liftingResult: Seq[External],
      liftingSize: Int
    ): (String, List[External]) = workList match {
      case Nil => sqlResult.reverse.mkString("") -> liftingResult.reverse.toList
      case head :: tail =>
        head match {
          case StringToken(s2)            => apply(tail, s2 +: sqlResult, liftingResult, liftingSize)
          case SetContainsToken(a, op, b) => apply(stmt"$a $op ($b)" +: tail, sqlResult, liftingResult, liftingSize)
          case ScalarLiftToken(lift) =>
            apply(tail, liftingPlaceholder(liftingSize) +: sqlResult, lift +: liftingResult, liftingSize + 1)
          case ScalarTagToken(tag) =>
            apply(tail, liftingPlaceholder(liftingSize) +: sqlResult, tag +: liftingResult, liftingSize + 1)
          case Statement(tokens)       => apply(tokens.foldRight(tail)(_ +: _), sqlResult, liftingResult, liftingSize)
          case ValuesClauseToken(stmt) => apply(stmt +: tail, sqlResult, liftingResult, liftingSize)
          case _: QuotationTagToken =>
            throw new UnsupportedOperationException("Quotation Tags must be resolved before a reification.")
        }
    }

    apply(List(token), Seq(), Seq(), 0)
  }

  private def expandLiftings[T](
    statement: Statement,
    emptySetContainsToken: Token => Token,
    subBatch: List[T],
    injectables: collection.Map[String, T => ScalarLift]
  ) = {

    def resolveInjectableValue(v: ScalarTagToken, value: T) = {
      val injectable =
        // Look up the right uuid:String to get the right <some-field> for ((p:Person) => ScalarLift(p.<some-field>))
        injectables.get(v.tag.uid) match {
          case Some(value) => value
          case None =>
            throw new IllegalArgumentException(
              s"No insert-values entity found for the id: ${v.tag.uid}. Existing injectable values are: ${injectables}"
            )
        }
      // Then plug in the (p:Person) value to get ScalarLift(p.<some-field>)
      val realLift = injectable(value)
      ScalarLiftToken(realLift)
    }

    // Find the correct uuid:String -> ((p:Person) => ScalarLift(p.name)) fora given uuid // i.e. ScalarLift(p.<field>)
    // then apply(Person) to (p:Person) => ScalarLift(p.name) etc...
    def plugScalarTags(token: Token, value: T): Token =
      token match {
        // Take the correct ((p:Person) => ScalarLift(p.name)) from the Map[uuid -> ((p:Person) => ScalarLift(p.<field-value>))] values
        // using the uuid of the tag. Then plug in the row-value i.e. `Person` to get ScalarLift(p.<field-value>))
        case tag: ScalarTagToken => resolveInjectableValue(tag, value)
        // Not supported in Scala2-Quill, don't really care
        case v: QuotationTagToken => v
        case v: StringToken       => v
        case v: ScalarLiftToken   => v
        case ValuesClauseToken(statement) =>
          ValuesClauseToken(Statement(statement.tokens.map(plugScalarTags(_, value))))
        case Statement(tokens) =>
          Statement(tokens.map(plugScalarTags(_, value)))
        // Don't see how lifts can be inside a set-contains but recursing here just in case
        case SetContainsToken(a, op, b) =>
          SetContainsToken(plugScalarTags(a, value), plugScalarTags(op, value), plugScalarTags(b, value))
      }

    Statement {
      statement.tokens.foldLeft(List.empty[Token]) {
        // If we are not doing batch lifting, that means there should only be ONE entity in the list
        case (tokens, tag: ScalarTagToken) =>
          if (subBatch.length != 1)
            throw new IllegalArgumentException(
              s"Expecting a batch of exactly one value for a non-VALUES-clause lift (e.g. for a context that does not support VALUES clauses) but found: ${subBatch}"
            )
          else {
            val resolvedLift = resolveInjectableValue(tag, subBatch.head)
            tokens :+ resolvedLift
          }
        case (tokens, valuesClause: ValuesClauseToken) =>
          val pluggedClauses = subBatch.map(value => plugScalarTags(valuesClause, value))
          val separators     = List.fill(pluggedClauses.size - 1)(StringToken(", "))
          (tokens ++ Interleave(pluggedClauses.toList, separators))
        case (tokens, SetContainsToken(a, op, ScalarLiftToken(lift: ScalarQueryLift))) =>
          lift.value.asInstanceOf[Iterable[Any]].toList match {
            case Nil => tokens :+ emptySetContainsToken(a)
            case values =>
              val liftings = values.map(v =>
                ScalarLiftToken(ScalarValueLift(lift.name, External.Source.Parser, v, lift.encoder, lift.quat))
              )
              val separators = List.fill(liftings.size - 1)(StringToken(", "))
              (tokens :+ stmt"$a $op (") ++ Interleave(liftings, separators) :+ StringToken(")")
          }
        case (tokens, token) =>
          tokens :+ token
      }
    }
  }
}
