package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.jdbczio.Quill
import javax.sql.DataSource

package object oracle {
  implicit val pool: DataSource = runLayerUnsafe(Quill.DataSource.fromPrefix("testOracleDB"))
  object testContext extends Quill.Oracle(Literal, pool) with TestEntities
}
