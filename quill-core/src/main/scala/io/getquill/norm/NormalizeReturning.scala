package io.getquill.norm

import io.getquill.ast._

object NormalizeReturning {

  def apply(e: Action): Action = {
    e match {
      case Returning(Insert(query, assignments), alias, body) =>
        Returning(Insert(query, filterReturnedColumn(assignments, body)), alias, body)
      case Returning(Update(query, assignments), alias, body) =>
        Returning(Update(query, filterReturnedColumn(assignments, body)), alias, body)
      case e => e
    }
  }

  private def filterReturnedColumn(assignments: List[Assignment], column: Ast): List[Assignment] =
    assignments.flatMap(filterReturnedColumn(_, column))

  private def filterReturnedColumn(assignment: Assignment, column: Ast): Option[Assignment] =
    (assignment, column) match {
      case (Assignment(_, Property(_, p1), _), Property(_, p2)) if (p1 == p2) =>
        None
      case (assignment, column) =>
        Some(assignment)
    }
}
