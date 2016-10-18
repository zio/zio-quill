package io.getquill

import java.util.concurrent.atomic.AtomicInteger

import io.getquill.ast._
import io.getquill.context.sql.idiom.{QuestionMarkBindVariables, SqlIdiom}
import io.getquill.idiom.StatementInterpolator._

trait PostgresDialect
  extends SqlIdiom
  with QuestionMarkBindVariables {

  override implicit def operationTokenizer(implicit propertyTokenizer: Tokenizer[Property], strategy: NamingStrategy): Tokenizer[Operation] =
    Tokenizer[Operation] {
      case UnaryOperation(StringOperator.`toLong`, ast) => stmt"${scopedTokenizer(ast)}::bigint"
      case UnaryOperation(StringOperator.`toInt`, ast)  => stmt"${scopedTokenizer(ast)}::integer"
      case operation                                    => super.operationTokenizer.token(operation)
    }

  private[getquill] val preparedStatementId = new AtomicInteger

  override def prepareForProbing(string: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet.toString.token} AS $string"

  /*
  override implicit def actionTokenizer(implicit strategy: NamingStrategy): Tokenizer[Action] = {
    Tokenizer[Action] {
      case Upsert(table: Entity, assignments) => super.actionTokenizer.token(Upsert(table, assignments))
      case action => super.actionTokenizer.token(action)
    }
  }
  */
}

object PostgresDialect extends PostgresDialect
