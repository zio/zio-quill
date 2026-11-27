package zio.quill

import zio.quill.idiom.StatementInterpolator._

import java.util.concurrent.atomic.AtomicInteger
import zio.quill.ast.{Ast, OnConflict}
import zio.quill.context.{CanInsertReturningWithMultiValues, CanInsertWithMultiValues, CanReturnField}
import zio.quill.context.sql.idiom.PositionalBindVariables
import zio.quill.context.sql.idiom.SqlIdiom
import zio.quill.context.sql.idiom.ConcatSupport
import zio.quill.util.Messages.fail

trait H2Dialect
    extends SqlIdiom
    with PositionalBindVariables
    with ConcatSupport
    with CanReturnField
    with CanInsertWithMultiValues
    with CanInsertReturningWithMultiValues {

  private[quill] val preparedStatementId = new AtomicInteger

  override def prepareForProbing(string: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet.toString.token} AS $string}"

  override def astTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case c: OnConflict => c.token
      case ast           => super.astTokenizer.token(ast)
    }

  implicit def conflictTokenizer(implicit
    astTokenizer: Tokenizer[Ast],
    strategy: NamingStrategy,
    idiomContext: IdiomContext
  ): Tokenizer[OnConflict] = {
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
