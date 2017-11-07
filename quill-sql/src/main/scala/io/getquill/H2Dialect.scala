package io.getquill

import io.getquill.idiom.StatementInterpolator._
import java.util.concurrent.atomic.AtomicInteger
import io.getquill.context.sql.idiom.PositionalBindVariables
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.idiom.ConcatSupport

trait H2Dialect
  extends SqlIdiom
  with PositionalBindVariables
  with ConcatSupport {

  private[getquill] val preparedStatementId = new AtomicInteger

  override def prepareForProbing(string: String) =
    s"PREPARE p${preparedStatementId.incrementAndGet.toString.token} AS $string}"
}

object H2Dialect extends H2Dialect
