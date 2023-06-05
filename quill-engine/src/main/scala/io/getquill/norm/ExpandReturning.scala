package io.getquill.norm

import io.getquill.ReturnAction.ReturnColumns
import io.getquill.ast.Renameable.Fixed
import io.getquill.ast._
import io.getquill.context._
import io.getquill.idiom.{Idiom, Statement}
import io.getquill.{NamingStrategy, ReturnAction, IdiomContext}

/**
 * Take the `.returning` part in a query that contains it and return the array
 * of columns representing of the returning section with any other operations
 * etc... that they might contain.
 */
object ExpandReturning {

  def applyMap(
    returning: ReturningAction
  )(f: (Ast, Statement) => String)(idiom: Idiom, naming: NamingStrategy, idiomContext: IdiomContext) =
    idiom.idiomReturningCapability match {
      case ReturningClauseSupported | OutputClauseSupported =>
        ReturnAction.ReturnRecord
      case ReturningMultipleFieldSupported =>
        val initialExpand = ExpandReturning(returning)(idiom, naming, idiomContext)
        ReturnColumns(initialExpand.map { case (ast, statement) => f(ast, statement) })
      case ReturningSingleFieldSupported =>
        val initialExpand = ExpandReturning(returning)(idiom, naming, idiomContext)
        if (initialExpand.length == 1)
          ReturnColumns(initialExpand.map { case (ast, statement) => f(ast, statement) })
        else
          throw new IllegalArgumentException(
            s"Only one RETURNING column is allowed in the ${idiom} dialect but ${initialExpand.length} were specified."
          )
      case ReturningNotSupported =>
        throw new IllegalArgumentException(s"RETURNING columns are not allowed in the ${idiom} dialect.")
    }

  def apply(
    returning: ReturningAction,
    renameAlias: Option[String] = None
  )(idiom: Idiom, naming: NamingStrategy, idiomContext: IdiomContext): List[(Ast, Statement)] = {
    val ReturningAction(_, alias, properties) = returning

    // Ident("j"), Tuple(List(Property(Ident("j"), "name"), BinaryOperation(Property(Ident("j"), "age"), +, Constant(1))))
    // => Tuple(List(ExternalIdent("name"), BinaryOperation(ExternalIdent("age"), +, Constant(1))))
    val dePropertized = renameAlias match {
      case Some(newName) =>
        BetaReduction(properties, alias -> ExternalIdent.Opinionated(newName, alias.quat, Fixed))
      case None =>
        BetaReduction(properties, alias -> ExternalIdent(alias.name, alias.quat))
    }

    // Tuple(List(ExternalIdent("name"), BinaryOperation(ExternalIdent("age"), +, Constant(1))))
    // => List(ExternalIdent("name"), BinaryOperation(ExternalIdent("age"), +, Constant(1)))
    val deTuplified = dePropertized match {
      case Tuple(values)        => values
      case CaseClass(_, values) => values.map(_._2)
      case other                => List(other)
    }

    implicit val namingStrategy: NamingStrategy = naming
    // TODO Should propagate ExecutionType from caller of this method. Need to trace
    val outputs = deTuplified.map(v => idiom.translate(v, dePropertized.quat, ExecutionType.Unknown, idiomContext))
    outputs.map { case (a, b, _) =>
      (a, b)
    }
  }
}
