package io.getquill

import java.util.concurrent.atomic.AtomicInteger
import io.getquill.context.sql.idiom.PositionalVariables
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.ast.UnaryOperation
import io.getquill.ast.Operation
import io.getquill.util.Show.Show
import io.getquill.ast.Property
import io.getquill.ast.StringOperator

trait PostgresDialect
  extends SqlIdiom
  with PositionalVariables {

  override implicit def operationShow(implicit propertyShow: Show[Property], strategy: NamingStrategy): Show[Operation] =
    Show[Operation] {
      case UnaryOperation(StringOperator.`toLong`, ast) => s"${scopedShow(ast)}::bigint"
      case UnaryOperation(StringOperator.`toInt`, ast)  => s"${scopedShow(ast)}::integer"
      case operation                                    => super.operationShow.show(operation)
    }

  private[getquill] val preparedStatementId = new AtomicInteger

  override def prepare(sql: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet} AS ${positionalVariables(sql)}"
}

object PostgresDialect extends PostgresDialect
