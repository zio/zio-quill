package io.getquill.norm

import io.getquill.ReturnAction.ReturnColumns
import io.getquill.{ NamingStrategy, ReturnAction }
import io.getquill.ast._
import io.getquill.context.{
  ReturningClauseSupported,
  ReturningMultipleFieldSupported,
  ReturningNotSupported,
  ReturningSingleFieldSupported
}
import io.getquill.idiom.{ Idiom, Statement }

/**
 * Take the `.returning` part in a query that contains it and return the array of columns
 * representing of the returning seccovtion with any other operations etc... that they might contain.
 */
object ExpandReturning {

  def applyMap(returning: ReturningAction)(f: (Ast, Statement) => String)(idiom: Idiom, naming: NamingStrategy) = {
    val initialExpand = ExpandReturning.apply(returning)(idiom, naming)

    idiom.idiomReturningCapability match {
      case ReturningClauseSupported =>
        ReturnAction.ReturnRecord
      case ReturningMultipleFieldSupported =>
        ReturnColumns(initialExpand.map { case (ast, statement) => f(ast, statement) })
      case ReturningSingleFieldSupported =>
        if (initialExpand.length == 1)
          ReturnColumns(initialExpand.map { case (ast, statement) => f(ast, statement) })
        else
          throw new IllegalArgumentException(s"Only one RETURNING column is allowed in the ${idiom} dialect but ${initialExpand.length} were specified.")
      case ReturningNotSupported =>
        throw new IllegalArgumentException(s"RETURNING columns are not allowed in the ${idiom} dialect.")
    }
  }

  def apply(returning: ReturningAction)(idiom: Idiom, naming: NamingStrategy): List[(Ast, Statement)] = {
    val ReturningAction(_, alias, properties) = returning

    // Ident("j"), Tuple(List(Property(Ident("j"), "name"), BinaryOperation(Property(Ident("j"), "age"), +, Constant(1))))
    // => Tuple(List(ExternalIdent("name"), BinaryOperation(ExternalIdent("age"), +, Constant(1))))
    val dePropertized =
      Transform(properties) {
        case `alias` => ExternalIdent(alias.name)
      }

    val aliasName = alias.name

    // Tuple(List(ExternalIdent("name"), BinaryOperation(ExternalIdent("age"), +, Constant(1))))
    // => List(ExternalIdent("name"), BinaryOperation(ExternalIdent("age"), +, Constant(1)))
    val deTuplified = dePropertized match {
      case Tuple(values)     => values
      case CaseClass(values) => values.map(_._2)
      case other             => List(other)
    }

    implicit val namingStrategy: NamingStrategy = naming
    deTuplified.map(v => idiom.translate(v))
  }
}
