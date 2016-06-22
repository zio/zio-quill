package io.getquill.norm

import io.getquill.ast.Action
import io.getquill.ast.AssignedAction
import io.getquill.ast.Assignment
import io.getquill.ast.Entity
import io.getquill.ast.Filter
import io.getquill.ast.Insert
import io.getquill.ast.StatelessTransformer
import io.getquill.ast.Update

object RenameAssignments extends StatelessTransformer {

  override def apply(e: Action): Action =
    e match {
      case AssignedAction(insert @ Insert(table: Entity), assignments) =>
        AssignedAction(insert, renameAssignments(assignments, table))

      case AssignedAction(update @ Update(table: Entity), assignments) =>
        AssignedAction(update, renameAssignments(assignments, table))

      case AssignedAction(update @ Update(Filter(table: Entity, x, where)), assignments) =>
        AssignedAction(update, renameAssignments(assignments, table))

      case other =>
        super.apply(other)
    }

  private def renameAssignments(assignments: List[Assignment], table: Entity) = {
    val propertyAlias = table.properties.map(p => p.property -> p.alias).toMap
    assignments.map(a => a.copy(property = propertyAlias.getOrElse(a.property, a.property)))
  }
}
