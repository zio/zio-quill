package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.ziojdbc.Quill

package object sqlserver {
  val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testSqlServerDB"))
  object testContext extends Quill.SqlServerService(Literal, pool) with TestEntities
}
