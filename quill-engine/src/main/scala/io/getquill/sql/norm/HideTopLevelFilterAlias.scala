package io.getquill.sql.norm

import io.getquill.ast._
import io.getquill.norm.BetaReduction

// Normally This:
//   query[Person].filter(p => p.name == 'Joe').update(_.name -> 'Jim', _.age = 123
// Actually becomes this:
//   UPDATE Person p SET (name, age) VALUES (p.name = 'Jim', p.age = 123) WHERE p.name = 'Joe'
// (see NormalizeFilteredActionAliases for more detail)

//
// However, when `SqlIdiom.useActionTableAliasAs=ActionTableAliasBehavior.Hide`
// It becomes something like this:
//   UPDATE Person /*p is hidden*/ SET (p.name = 'Jim', p.age = 123) WHERE p.name = 'Joe'
// So we need to make it become this:
//   UPDATE Person /*p is hidden*/ SET (name = 'Jim', age = 123) WHERE name = 'Joe'
//
// Note that this is the case with Oracle. With SqlServer we also use this functionality
// but with output causes the alias becomes OUTPUT so it can be different in those cases.
object HideTopLevelFilterAlias extends StatelessTransformer {
  def hideAlias(alias: Ident, in: Ast) = {
    val newAlias = Ident.Opinionated(alias.name, alias.quat, Visibility.Hidden, alias.pos)
    (newAlias, BetaReduction(in, alias -> newAlias))
  }

  def hideAssignmentAlias(assignment: Assignment) = {
    val alias         = assignment.alias
    val newAlias      = Ident.Opinionated(alias.name, alias.quat, Visibility.Hidden, alias.pos)
    val newValue      = BetaReduction(assignment.value, alias -> newAlias)
    val newProperty   = BetaReduction(assignment.property, alias -> newAlias)
    val newAssignment = Assignment(newAlias, newProperty, newValue)
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
