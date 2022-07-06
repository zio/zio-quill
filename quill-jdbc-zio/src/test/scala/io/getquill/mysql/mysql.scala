package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object mysql {
  implicit val pool = zio.Unsafe.unsafe { implicit u => Implicit(zio.Runtime.unsafe.fromLayer(DataSourceLayer.fromPrefix("testMysqlDB"))) }
  object testContext extends MysqlZioJdbcContext(Literal) with TestEntities
}
