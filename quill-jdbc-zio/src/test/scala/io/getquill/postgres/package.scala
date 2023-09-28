package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.jdbczio.Quill
import javax.sql.DataSource

package object postgres {
  val pool: DataSource = runLayerUnsafe(Quill.DataSource.fromPrefix("testPostgresDB"))
  object testContext extends Quill.Postgres(Literal, pool) with TestEntities
}
