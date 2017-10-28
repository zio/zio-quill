package io.getquill

import java.util.concurrent.atomic.AtomicInteger

import io.getquill.ast._
import io.getquill.context.sql.idiom.{ QuestionMarkBindVariables, SqlIdiom }
import io.getquill.idiom.StatementInterpolator._
import io.getquill.context.sql.idiom.ConcatSupport

trait PostgresDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport {

  override def astTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Ast] =
    Tokenizer[Ast] {
      case ListContains(ast, body) => stmt"${body.token} = ANY(${ast.token})"
      case ast                     => super.astTokenizer.token(ast)
    }

  override implicit def operationTokenizer(implicit astTokenizer: Tokenizer[Ast], strategy: NamingStrategy): Tokenizer[Operation] =
    Tokenizer[Operation] {
      case UnaryOperation(StringOperator.`toLong`, ast) => stmt"${scopedTokenizer(ast)}::bigint"
      case UnaryOperation(StringOperator.`toInt`, ast)  => stmt"${scopedTokenizer(ast)}::integer"
      case operation                                    => super.operationTokenizer.token(operation)
    }

  private[getquill] val preparedStatementId = new AtomicInteger

  override def prepareForProbing(string: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet.toString.token} AS $string"
}

object PostgresDialect extends PostgresDialect
