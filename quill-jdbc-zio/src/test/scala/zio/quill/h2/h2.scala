package zio.quill

import zio.quill.ZioSpec.runLayerUnsafe
import zio.quill.jdbczio.Quill

package object h2 {
  val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testH2DB"))
  object testContext extends Quill.H2(Literal, pool) with TestEntities
}
