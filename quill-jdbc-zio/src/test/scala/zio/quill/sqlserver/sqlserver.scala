package zio.quill

import zio.quill.ZioSpec.runLayerUnsafe
import zio.quill.jdbczio.Quill

package object sqlserver {
  val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testSqlServerDB"))
  object testContext extends Quill.SqlServer(Literal, pool) with TestEntities
}
