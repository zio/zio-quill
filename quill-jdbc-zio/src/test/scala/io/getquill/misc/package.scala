package io.getquill

import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object misc {
  implicit val pool = Implicit(io.getquill.postgres.pool)
  object testContext extends PostgresZioJdbcContext(Literal) with TestEntities
}
