package io.getquill.sql.idiom

import io.getquill.NamingStrategy
import io.getquill.ast._
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.idiom.SqlIdiom.ActionTableAliasBehavior
import io.getquill.idiom.StatementInterpolator._
import io.getquill.norm.BetaReduction

trait NoActionAliases extends SqlIdiom {

  override def useActionTableAliasAs = ActionTableAliasBehavior.Hide

  object HideTopLevelFilterAlias extends StatelessTransformer {
    def hideAlias(alias: Ident, in: Ast) = {
      val newAlias = Ident.Opinionated(alias.name, alias.quat, Visibility.Hidden)
      (newAlias, BetaReduction(in, alias -> newAlias))
    }

    def hideAssignmentAlias(assignment: Assignment) = {
      import io.getquill.util.Messages.qprint
      val alias = assignment.alias
      val newAlias = Ident.Opinionated(alias.name, alias.quat, Visibility.Hidden)
      val newValue = BetaReduction(assignment.value, alias -> newAlias)
      val newProperty = BetaReduction(assignment.property, alias -> newAlias)
      val newAssignment = Assignment(newAlias, newProperty, newValue)

      println(
        s"""=============== HIDING ALIAS =========
           |${qprint(assignment.alias)}
           |IN
           |${qprint(assignment)}
           |RESULT
           |${qprint(newAssignment)}""".stripMargin
      )
      newAssignment.asInstanceOf[Assignment]
    }

    override def apply(e: Action): Action =
      e match {
        case Update(Filter(query, alias, body), assignments) =>
          val (newAlias, newBody) = hideAlias(alias, body)
          Update(Filter(query, newAlias, newBody), assignments.map(hideAssignmentAlias(_)))

        case Delete(Filter(query, alias, body)) =>
          val (newAlias, newBody) = hideAlias(alias, body)
          Delete(Filter(query, newAlias, newBody))

        case _ => super.apply(e)
      }
  }
}
