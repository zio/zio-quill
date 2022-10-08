package io.getquill.sql.norm

import io.getquill.ast._
import io.getquill.norm.BetaReduction

object NormalizeFilteredActionAliases {
  private[getquill] def chooseAlias(entityName: String, batchAlias: Option[String]) = {
    val lowerEntityName = entityName.toLowerCase
    val possibleEntityNameChar =
      if (lowerEntityName.length > 0 && lowerEntityName.take(1).matches("[a-z]"))
        Some(lowerEntityName.take(1))
      else
        None
    (possibleEntityNameChar, batchAlias) match {
      case (Some(t), Some(b)) if (b != t) => t
      case (Some(t), Some(b)) if (b == t) => s"${t}Tbl"
      case (Some(t), None)                => t
      case (None, Some(b))                => s"${b}Tbl"
      case (None, None)                   => "x"
    }
  }
}

/** In actions inner properties typically result from embedded classes, hide them */
object HideInnerProperties extends StatelessTransformer {
  override def apply(e: Property): Property =
    e.copyAll(ast = recurseHide(e.ast))

  // Note should also transformation for properties where queries are in the Returning(...) slot of actions.
  // Their inner properties should be hidden by select expansion anyway.
  private def recurseHide(ast: Ast): Ast =
    ast match {
      case p @ Property(inner, _) =>
        val innerNew = recurseHide(inner)
        p.copyAll(ast = innerNew, visibility = Visibility.Hidden)
      case _ => ast
    }
}

case class NormalizeFilteredActionAliases(batchAlias: Option[String]) extends StatelessTransformer {

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

      case Update(e: Entity, assignments) =>
        val alias = NormalizeFilteredActionAliases.chooseAlias(e.name, batchAlias)
        Update(e, assignments.map(a => realiasAssignment(a, alias)))

      case _ => super.apply(e)
    }

  private def realiasAssignment(a: Assignment, newAlias: Ident) =
    a match {
      case Assignment(alias, prop, value) =>
        // Beta reduction will not swap an alias out from under a property so need to use a full transform
        val newProp = Transform(prop) { case `alias` => newAlias }
        val newVal = Transform(value) { case `alias` => newAlias }
        Assignment(newAlias, newProp, newVal)
    }

  private def realiasAssignment(a: Assignment, newAliasName: String) =
    a match {
      case Assignment(alias, prop, value) =>
        val newAlias = alias.copy(name = newAliasName)
        val newProp = Transform(prop) { case `alias` => newAlias }
        val newVal = Transform(value) { case `alias` => newAlias }
        Assignment(newAlias, newProp, newVal)
    }
}
