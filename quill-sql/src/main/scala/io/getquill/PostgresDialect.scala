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
  This doesn't work correctly...
  INSERT INTO TestEntity (v.s,v.l,v.o) VALUES (?, ?, ?) ON CONFLICT(x1.i) DO UPDATE SET x2.i = ?, x3.l = ?, x4.s = ?

  override implicit def actionTokenizer(implicit strategy: NamingStrategy): Tokenizer[Action] = {
    Tokenizer[Action] {
      case Upsert(table: Entity, assignments) =>
        val columns = assignments.map(_.property.token)
        val values = assignments.map(_.value)
        stmt"INSERT INTO ${table.token} (${columns.mkStmt(",")}) VALUES (${values.map(scopedTokenizer(_)).mkStmt(", ")})"

      case Conflict(action, prop, value) => stmt"${action.token} ON CONFLICT(${value.token})"

      case ConflictUpdate(action, assignments) => stmt"${action.token} DO UPDATE SET ${assignments.mkStmt()}"

      case action => super.actionTokenizer.token(action)
    }
  }
  */
}

object PostgresDialect extends PostgresDialect
