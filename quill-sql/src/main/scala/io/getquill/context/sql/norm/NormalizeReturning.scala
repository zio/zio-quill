package io.getquill.context.sql.norm

import io.getquill.ast._

object NormalizeReturning extends StatelessTransformer {
  override def apply(e: Action): Action = {
    e match {
      case AssignedAction(FunctionApply(Returning(action, returning), values), assignments) =>
        AssignedAction(FunctionApply(action, values), assignments.filterNot(_.property == returning))
      case Returning(AssignedAction(action, assignments), returning) =>
        AssignedAction(action, assignments.filterNot(_.property == returning))
      case _ => super.apply(e)
    }
  }
}
