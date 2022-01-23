package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object postgres {
  implicit val pool = Implicit(zio.Runtime.unsafeFromLayer(DataSourceLayer.fromPrefix("testPostgresDB")))
  object testContext extends PostgresZioJdbcContext(Literal) with TestEntities
}
