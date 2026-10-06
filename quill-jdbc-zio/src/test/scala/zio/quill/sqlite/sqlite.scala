package zio.quill

import zio.quill.ZioSpec.runLayerUnsafe
import zio.quill.jdbczio.Quill

package object sqlite {
  val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testSqliteDB"))
  object testContext extends Quill.Sqlite(Literal, pool) with TestEntities
}
