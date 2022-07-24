package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.ziojdbc.Quill

package object postgres {
  val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testPostgresDB"))
  object testContext extends Quill.PostgresService(Literal, pool) with TestEntities
}
