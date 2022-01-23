package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object sqlserver {
  implicit val pool = Implicit(zio.Runtime.unsafeFromLayer(DataSourceLayer.fromPrefix("testSqlServerDB")))
  object testContext extends SqlServerZioJdbcContext(Literal) with TestEntities
}
