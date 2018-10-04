package io.getquill

import io.getquill.ast._
import io.getquill.context.sql.idiom.{ NoConcatSupport, QuestionMarkBindVariables, SqlIdiom }
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.{ StringToken, Token }
import io.getquill.util.Messages.fail

trait SqliteDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with NoConcatSupport {

  override def emptySetContainsToken(field: Token) = StringToken("0")

  override def prepareForProbing(string: String) = s"sqlite3_prepare_v2($string)"

  override def astTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case c: OnConflict => conflictTokenizer.token(c)
      case ast           => super.astTokenizer.token(ast)
    }

  implicit def conflictTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[OnConflict] = {

    val customEntityTokenizer = Tokenizer[Entity] {
      case Entity(name, _) => stmt"INTO ${strategy.table(name).token} AS t"
    }

    val customAstTokenizer =
      Tokenizer.withFallback[Ast](SqliteDialect.this.astTokenizer(_, strategy)) {
        case _: OnConflict.Excluded => stmt"EXCLUDED"
        case OnConflict.Existing(a) => stmt"${a.token}"
        case a: Action              => super.actionTokenizer(customEntityTokenizer)(actionAstTokenizer, strategy).token(a)
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
        case OnConflict.Properties(props) => stmt"(${props.map(n => strategy.column(n.name)).mkStmt(",")})"
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

object SqliteDialect extends SqliteDialect
