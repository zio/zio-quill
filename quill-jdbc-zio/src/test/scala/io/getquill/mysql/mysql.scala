package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object mysql {
  implicit val pool = Implicit(zio.Runtime.unsafeFromLayer(DataSourceLayer.fromPrefix("testMysqlDB")))
  object testContext extends MysqlZioJdbcContext(Literal) with TestEntities
}
