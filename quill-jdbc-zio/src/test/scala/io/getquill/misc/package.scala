package io.getquill

import io.getquill.context.qzio.ImplicitSyntax.Implicit
import javax.sql.DataSource

package object misc {
  implicit val pool: Implicit[DataSource] = Implicit(io.getquill.postgres.pool)
  object testContext extends PostgresZioJdbcContext(Literal) with TestEntities
}
