package io.getquill

import java.util.concurrent.atomic.AtomicInteger

import io.getquill.ast._
import io.getquill.context.CanReturnClause
import io.getquill.context.sql.idiom._
import io.getquill.idiom.StatementInterpolator._

trait PostgresDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with OnConflictSupport
  with CanReturnClause {

  override def astTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case ListContains(ast, body) => stmt"${body.token} = ANY(${ast.token})"
      case c: OnConflict           => conflictTokenizer.token(c)
      case ast                     => super.astTokenizer.token(ast)
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
