package io.getquill.sql.idiom

import io.getquill.NamingStrategy
import io.getquill.ast._
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.idiom.StatementInterpolator._
import io.getquill.norm.BetaReduction

trait NoActionAliases extends SqlIdiom {

  object HideTopLevelFilterAlias extends StatelessTransformer {
    override def apply(e: Action): Action =
      e match {
        case Update(Filter(query, alias, body), assignments) =>
          val newAlias = Ident.Opinionated(alias.name, alias.quat, Visibility.Hidden)
          val newBody = BetaReduction(body, alias -> newAlias)
          Update(Filter(query, newAlias, newBody), assignments)

        case Delete(Filter(query, alias, body)) =>
          val newAlias = Ident.Opinionated(alias.name, alias.quat, Visibility.Hidden)
          val newBody = BetaReduction(body, alias -> newAlias)
          Delete(Filter(query, newAlias, newBody))

        case _ => super.apply(e)
      }
  }

  // Hide table aliases
  override protected def actionTokenizer(insertEntityTokenizer: Tokenizer[Entity])(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Action] =
    Tokenizer[Action] {
      // Update(Filter(...)) and Delete(Filter(...)) usually cause a table alias i.e. `UPDATE People <alias> SET ... WHERE ...` or `DELETE FROM People <alias> WHERE ...`
      // since the alias is used in the WHERE clause. This functionality removes that because Oracle doesn't support aliasing in actions.
      case Update(Filter(table: Entity, x, where), assignments) =>
        stmt"UPDATE ${table.token} SET ${assignments.token} WHERE ${where.token}"
      case Delete(Filter(table: Entity, x, where)) =>
        stmt"DELETE FROM ${table.token} WHERE ${where.token}"
      case other => super.actionTokenizer(insertEntityTokenizer).token(other)
    }
}
