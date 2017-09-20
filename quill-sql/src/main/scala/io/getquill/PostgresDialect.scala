package io.getquill

import io.getquill.idiom.StatementInterpolator._
import java.util.concurrent.atomic.AtomicInteger
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.ast.UnaryOperation
import io.getquill.ast.Operation
import io.getquill.ast.StringOperator
import io.getquill.context.sql.idiom.QuestionMarkBindVariables
import io.getquill.ast.Ast

trait PostgresDialect
  extends SqlIdiom
  with QuestionMarkBindVariables {

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
