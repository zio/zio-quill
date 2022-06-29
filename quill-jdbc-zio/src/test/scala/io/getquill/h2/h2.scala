package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object h2 {
  implicit val pool = zio.Unsafe.unsafe { implicit u => Implicit(zio.Runtime.unsafe.fromLayer(DataSourceLayer.fromPrefix("testH2DB"))) }
  object testContext extends H2ZioJdbcContext(Literal) with TestEntities
}
