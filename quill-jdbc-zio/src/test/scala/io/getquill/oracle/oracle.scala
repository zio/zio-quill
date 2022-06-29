package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object oracle {
  implicit val pool = zio.Unsafe.unsafe { implicit u => Implicit(zio.Runtime.unsafe.fromLayer(DataSourceLayer.fromPrefix("testOracleDB"))) }
  object testContext extends OracleZioJdbcContext(Literal) with TestEntities
}
