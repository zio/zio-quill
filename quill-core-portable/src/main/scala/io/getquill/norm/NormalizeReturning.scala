package io.getquill.norm

import io.getquill.ast._
import io.getquill.norm.capture.AvoidAliasConflict

/**
 * When actions are used with a `.returning` clause, remove the columns used in the returning clause from the action.
 * E.g. for `insert(Person(id, name)).returning(_.id)` remove the `id` column from the original insert.
 */
object NormalizeReturning {

  def apply(e: Action): Action = {
    e match {
      case ReturningGenerated(a: Action, alias, body) =>
        // De-alias the body first so variable shadows won't accidentally be interpreted as columns to remove from the insert/update action.
        // This typically occurs in advanced cases where actual queries are used in the return clauses which is only supported in Postgres.
        // For example:
        // query[Entity].insert(lift(Person(id, name))).returning(t => (query[Dummy].map(t => t.id).max))
        // Since the property `t.id` is used both for the `returning` clause and the query inside, it can accidentally
        // be seen as a variable used in `returning` hence excluded from insertion which is clearly not the case.
        // In order to fix this, we need to change `t` into a different alias.
        val newBody = dealiasBody(body, alias)
        ReturningGenerated(apply(a, newBody, alias), alias, newBody)

      // For a regular return clause, do not need to exclude assignments from insertion however, we still
      // need to de-alias the Action body in case conflicts result. For example the following query:
      // query[Entity].insert(lift(Person(id, name))).returning(t => (query[Dummy].map(t => t.id).max))
      // would incorrectly be interpreted as:
      // INSERT INTO Person (id, name) VALUES (1, 'Joe') RETURNING (SELECT MAX(id) FROM Dummy t) -- Note the 'id' in max which is coming from the inserted table instead of t
      // whereas it should be:
      // INSERT INTO Entity (id) VALUES (1) RETURNING (SELECT MAX(t.id) FROM Dummy t1)
      case Returning(a: Action, alias, body) =>
        val newBody = dealiasBody(body, alias)
        Returning(a, alias, newBody)

      case _ => e
    }
  }

  /**
   * In some situations, a query can exist inside of a `returning` clause. In this case, we need to rename
   * if the aliases used in that query override the alias used in the `returning` clause otherwise
   * they will be treated as returning-clause aliases ExpandReturning (i.e. they will become ExternalAlias instances)
   * and later be tokenized incorrectly.
   */
  private def dealiasBody(body: Ast, alias: Ident): Ast =
    Transform(body) {
      case q: Query => AvoidAliasConflict.sanitizeQuery(q, Set(alias))
    }

  private def apply(e: Action, body: Ast, returningIdent: Ident): Action = e match {
    case Insert(query, assignments)         => Insert(query, filterReturnedColumn(assignments, body, returningIdent))
    case Update(query, assignments)         => Update(query, filterReturnedColumn(assignments, body, returningIdent))
    case OnConflict(a: Action, target, act) => OnConflict(apply(a, body, returningIdent), target, act)
    case _                                  => e
  }

  private def filterReturnedColumn(assignments: List[Assignment], column: Ast, returningIdent: Ident): List[Assignment] =
    assignments.flatMap(filterReturnedColumn(_, column, returningIdent))

  /**
   * In situations like Property(Property(ident, foo), bar) pull out the inner-most ident
   */
  object NestedProperty {
    def unapply(ast: Property): Option[Ast] = {
      ast match {
        case p @ Property(subAst, _) => Some(innerMost(subAst))
        case _                       => None
      }
    }

    private def innerMost(ast: Ast): Ast = ast match {
      case Property(inner, _) => innerMost(inner)
      case other              => other
    }
  }

  /**
   * Remove the specified column from the assignment. For example, in a query like `insert(Person(id, name)).returning(r => r.id)`
   * we need to remove the `id` column from the insertion. The value of the `column:Ast` in this case will be `Property(Ident(r), id)`
   * and the values fo the assignment `p1` property will typically be `v.id` and `v.name` (the `v` variable is a default
   * used for `insert` queries).
   */
  private def filterReturnedColumn(assignment: Assignment, body: Ast, returningIdent: Ident): Option[Assignment] =
    assignment match {
      case Assignment(_, p1: Property, _) => {
        // Pull out instance of the column usage. The `column` ast will typically be Property(table, field) but
        // if the user wants to return multiple things it can also be a tuple Tuple(List(Property(table, field1), Property(table, field2))
        // or it can even be a query since queries are allowed to be in return sections e.g:
        // query[Entity].insert(lift(Person(id, name))).returning(r => (query[Dummy].filter(t => t.id == r.id).max))
        // In all of these cases, we need to pull out the Property (e.g. t.id) in order to compare it to the assignment
        // in order to know what to exclude.
        val matchedProps =
          CollectAst(body) {
            //case prop @ NestedProperty(`returningIdent`) => prop
            case prop @ NestedProperty(Ident(name)) if (name == returningIdent.name)         => prop
            case prop @ NestedProperty(ExternalIdent(name)) if (name == returningIdent.name) => prop
          }

        if (matchedProps.exists(matchedProp => isSameProperties(p1, matchedProp)))
          None
        else
          Some(assignment)
      }
      case assignment => Some(assignment)
    }

  object SomeIdent {
    def unapply(ast: Ast): Option[Ast] =
      ast match {
        case id: Ident         => Some(id)
        case id: ExternalIdent => Some(id)
        case _                 => None
      }
  }

  /**
   * Is it the same property (but possibly of a different identity). E.g. `p.foo.bar` and `v.foo.bar`
   */
  private def isSameProperties(p1: Property, p2: Property): Boolean = (p1.ast, p2.ast) match {
    case (SomeIdent(_), SomeIdent(_)) =>
      p1.name == p2.name
    // If it's Property(Property(Id), name) == Property(Property(Id), name) we need to check that the
    // outer properties are the same before moving on to the inner ones.
    case (pp1: Property, pp2: Property) if (p1.name == p2.name) =>
      isSameProperties(pp1, pp2)
    case _ =>
      false
  }
}
