package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.jdbczio.Quill
import javax.sql.DataSource

package object sqlserver {
  val pool: DataSource = runLayerUnsafe(Quill.DataSource.fromPrefix("testSqlServerDB"))
  object testContext extends Quill.SqlServer(Literal, pool) with TestEntities
}
