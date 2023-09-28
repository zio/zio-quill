package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.jdbczio.Quill
import javax.sql.DataSource

package object h2 {
  val pool: DataSource = runLayerUnsafe(Quill.DataSource.fromPrefix("testH2DB"))
  object testContext extends Quill.H2(Literal, pool) with TestEntities
}
