package io.getquill

import io.getquill.idiom.StatementInterpolator._
import java.util.concurrent.atomic.AtomicInteger

import io.getquill.ast.{ Ast, OnConflict }
import io.getquill.context.CanReturnField
import io.getquill.context.sql.idiom.PositionalBindVariables
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.idiom.ConcatSupport
import io.getquill.util.Messages.fail

trait H2Dialect
  extends SqlIdiom
  with PositionalBindVariables
  with ConcatSupport
  with CanReturnField {

  private[getquill] val preparedStatementId = new AtomicInteger

  override def prepareForProbing(string: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet.toString.token} AS $string}"

  override def astTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case c: OnConflict => c.token
      case ast           => super.astTokenizer.token(ast)
    }

  implicit def conflictTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[OnConflict] = {
    import OnConflict._
    def tokenizer(implicit astTokenizer: Tokenizer[Ast]) =
      Tokenizer[OnConflict] {
        case OnConflict(i, NoTarget, Ignore) => stmt"${astTokenizer.token(i)} ON CONFLICT DO NOTHING"
        case _                               => fail("Only onConflictIgnore upsert is supported in H2 (v1.4.200+).")
      }

    tokenizer(super.astTokenizer)
  }
}

object H2Dialect extends H2Dialect
