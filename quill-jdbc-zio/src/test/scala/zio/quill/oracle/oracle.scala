package zio.quill

import zio.quill.ZioSpec.runLayerUnsafe
import zio.quill.jdbczio.Quill

package object oracle {
  implicit val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testOracleDB"))
  object testContext extends Quill.Oracle(Literal, pool) with TestEntities
}
