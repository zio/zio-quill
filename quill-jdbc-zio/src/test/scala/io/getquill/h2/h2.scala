package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.jdbczio.Quill

package object h2 {
  val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testH2DB"))
  object testContext extends Quill.H2Service(Literal, pool) with TestEntities
}
