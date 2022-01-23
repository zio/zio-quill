package io.getquill

import io.getquill.context.ZioJdbc.DataSourceLayer
import io.getquill.context.qzio.ImplicitSyntax.Implicit

package object sqlite {
  implicit val pool = Implicit(zio.Runtime.unsafeFromLayer(DataSourceLayer.fromPrefix("testSqliteDB")))
  object testContext extends SqliteZioJdbcContext(Literal) with TestEntities
}
