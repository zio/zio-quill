package io.getquill.sql.norm

import io.getquill.ast._
import io.getquill.norm.BetaReduction

object NormalizeFilteredActionAliases extends StatelessTransformer {

  override def apply(e: Action): Action =
    e match {
      case Update(query @ Filter(_: Entity, alias, _), assignments) =>
        // This:
        //   query[Person].filter(p => p.name == 'Joe').update(_.name -> 'Jim', _.age = 123
        // Becomes something like this:
        //   UPDATE Person p SET (p.name, p.age) VALUES (x1 => x1.name = 'Jim', x1 => x2.age = 123) WHERE p.name = 'Joe'
        // Becomes this:
        //   UPDATE Person p SET (p.name, p.age) VALUES (x1.name = 'Jim', x2.age = 123) WHERE p.name = 'Joe'
        // Actually becomes this:
        //   UPDATE Person p SET (name, age) VALUES (x1.name = 'Jim', x2.age = 123) WHERE p.name = 'Joe'
        // (since we don't tokenize the identifier of the SET-clauses)

        // We need to change it to this:
        //   query[Person].filter(p => p.name == 'Joe').update(p => p.name -> 'Jim', p => p.age = 123
        // Becomes something like this:
        //   UPDATE Person p SET (p.name, p.age) VALUES (p => p.name = 'Jim', p => p.age = 123) WHERE p.name = 'Joe'
        // Becomes this:
        //   UPDATE Person p SET (p.name, p.age) VALUES (p.name = 'Jim', p.age = 123) WHERE p.name = 'Joe'
        // Actually becomes this:
        //   UPDATE Person p SET (name, age) VALUES (p.name = 'Jim', p.age = 123) WHERE p.name = 'Joe'
        // (since we don't tokenize the identifier of the SET-clauses)

        Update(apply(query), assignments.map(a => realiasAssignment(a, alias)))
      case _ => super.apply(e)
    }

  private def realiasAssignment(a: Assignment, newAlias: Ident) =
    a match {
      case Assignment(alias, prop, value) =>
        val newProp = BetaReduction(prop, alias -> newAlias)
        Assignment(newAlias, newProp, value)
    }
}
