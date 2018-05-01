package io.getquill

import java.util.concurrent.atomic.AtomicInteger

import io.getquill.ast._
import io.getquill.context.sql.idiom.{ ConcatSupport, QuestionMarkBindVariables, SqlIdiom }
import io.getquill.idiom.StatementInterpolator._
import io.getquill.idiom.Token
import io.getquill.util.Messages.fail

trait PostgresDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport {

  override def astTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case ListContains(ast, body) => stmt"${body.token} = ANY(${ast.token})"
      case c: OnConflict           => conflictTokenizer.token(c)
      case ast                     => super.astTokenizer.token(ast)
    }

  implicit def conflictTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[OnConflict] = {

    val customEntityTokenizer = Tokenizer[Entity] {
      case Entity(name, _) => stmt"INTO ${strategy.table(name).token} AS t"
    }

    val customAstTokenizer =
      Tokenizer.withFallback[Ast](PostgresDialect.this.astTokenizer(_, strategy)) {
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

  override implicit def operationTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Operation] =
    Tokenizer[Operation] {
      case UnaryOperation(StringOperator.`toLong`, ast) => stmt"${scopedTokenizer(ast)}::bigint"
      case UnaryOperation(StringOperator.`toInt`, ast)  => stmt"${scopedTokenizer(ast)}::integer"
      case operation                                    => super.operationTokenizer.token(operation)
    }

  private[getquill] val preparedStatementId = new AtomicInteger

  override def prepareForProbing(string: String) = {
    var i = 0
    val query = string.flatMap(x => if (x != '?') s"$x" else {
      i += 1
      s"$$$i"
    })
    s"PREPARE p${preparedStatementId.incrementAndGet.toString.token} AS $query"
  }
}

object PostgresDialect extends PostgresDialect
