package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object h2 {
  implicit val pool = Implicit(zio.Runtime.unsafeFromLayer(DataSourceLayer.fromPrefix("testH2DB")))
  object testContext extends H2ZioJdbcContext(Literal) with TestEntities
}
