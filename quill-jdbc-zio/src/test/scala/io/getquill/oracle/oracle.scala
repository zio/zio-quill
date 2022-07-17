package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.ziojdbc.Quill

package object oracle {
  implicit val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testOracleDB"))
  object testContext extends Quill.OracleService(Literal, pool) with TestEntities
}
