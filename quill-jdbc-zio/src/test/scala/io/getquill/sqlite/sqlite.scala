package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.jdbczio.Quill
import javax.sql.DataSource

package object sqlite {
  val pool: DataSource = runLayerUnsafe(Quill.DataSource.fromPrefix("testSqliteDB"))
  object testContext extends Quill.Sqlite(Literal, pool) with TestEntities
}
