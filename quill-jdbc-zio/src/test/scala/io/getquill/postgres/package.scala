package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object postgres {
  implicit val pool = zio.Unsafe.unsafe { implicit u => Implicit(zio.Runtime.unsafe.fromLayer(DataSourceLayer.fromPrefix("testPostgresDB"))) }
  object testContext extends PostgresZioJdbcContext(Literal) with TestEntities
}
