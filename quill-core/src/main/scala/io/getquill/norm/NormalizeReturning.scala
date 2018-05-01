package io.getquill.norm

import io.getquill.ast._

object NormalizeReturning {

  def apply(e: Action): Action = {
    e match {
      case Returning(a: Action, alias, body) => Returning(apply(a, body), alias, body)
      case _                                 => e
    }
  }

  private def apply(e: Action, body: Ast): Action = e match {
    case Insert(query, assignments)         => Insert(query, filterReturnedColumn(assignments, body))
    case Update(query, assignments)         => Update(query, filterReturnedColumn(assignments, body))
    case OnConflict(a: Action, target, act) => OnConflict(apply(a, body), target, act)
    case _                                  => e
  }

  private def filterReturnedColumn(assignments: List[Assignment], column: Ast): List[Assignment] =
    assignments.flatMap(filterReturnedColumn(_, column))

  private def filterReturnedColumn(assignment: Assignment, column: Ast): Option[Assignment] =
    (assignment, column) match {
      case (Assignment(_, p1: Property, _), p2: Property) if p1.name == p2.name && isSameProperties(p1, p2) =>
        None
      case (assignment, column) =>
        Some(assignment)
    }

  private def isSameProperties(p1: Property, p2: Property): Boolean = (p1.ast, p2.ast) match {
    case (_: Ident, _: Ident)           => p1.name == p2.name
    case (pp1: Property, pp2: Property) => isSameProperties(pp1, pp2)
    case _                              => false
  }
}
