package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object sqlite {
  implicit val pool = zio.Unsafe.unsafe { implicit u => Implicit(zio.Runtime.unsafe.fromLayer(DataSourceLayer.fromPrefix("testSqliteDB"))) }
  object testContext extends SqliteZioJdbcContext(Literal) with TestEntities
}
