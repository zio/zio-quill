package zio.quill

import zio.quill.ZioSpec.runLayerUnsafe
import zio.quill.jdbczio.Quill

package object postgres {
  val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testPostgresDB"))
  object testContext extends Quill.Postgres(Literal, pool) with TestEntities
}
