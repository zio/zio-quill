package io.getquill.context.sql.idiom

import io.getquill.ast._
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.Token
import io.getquill.NamingStrategy
import io.getquill.util.Messages.fail

trait OnConflictSupport {
  self: SqlIdiom =>

  implicit def conflictTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[OnConflict] = {

    val customEntityTokenizer = Tokenizer[Entity] {
      case Entity.Opinionated(name, _, _, renameable) => stmt"INTO ${renameable.fixedOr(name.token)(strategy.table(name).token)} AS t"
    }

    val customAstTokenizer =
      Tokenizer.withFallback[Ast](self.astTokenizer(_, strategy)) {
        case _: OnConflict.Excluded => stmt"EXCLUDED"
        case OnConflict.Existing(a) => stmt"${a.token}"
        case a: Action              => self.actionTokenizer(customEntityTokenizer)(actionAstTokenizer, strategy).token(a)
      }

    import OnConflict._

    def doUpdateStmt(i: Token, t: Token, u: Update) = {
      val assignments = u.assignments
        .map(a => stmt"${actionAstTokenizer.token(a.property)} = ${scopedTokenizer(a.value)(customAstTokenizer)}")
        .mkStmt()

      stmt"$i ON CONFLICT $t DO UPDATE SET $assignments"
    }

    def doNothingStmt(i: Ast, t: Token) = stmt"${i.token} ON CONFLICT $t DO NOTHING"

    implicit val conflictTargetPropsTokenizer =
      Tokenizer[Properties] {
        case OnConflict.Properties(props) => stmt"(${props.map(n => n.renameable.fixedOr(n.name)(strategy.column(n.name))).mkStmt(",")})"
      }

    def tokenizer(implicit astTokenizer: Tokenizer[Ast]) =
      Tokenizer[OnConflict] {
        case OnConflict(_, NoTarget, _: Update)      => fail("'DO UPDATE' statement requires explicit conflict target")
        case OnConflict(i, p: Properties, u: Update) => doUpdateStmt(i.token, p.token, u)

        case OnConflict(i, NoTarget, Ignore)         => stmt"${astTokenizer.token(i)} ON CONFLICT DO NOTHING"
        case OnConflict(i, p: Properties, Ignore)    => doNothingStmt(i, p.token)
      }

    tokenizer(customAstTokenizer)
  }
}