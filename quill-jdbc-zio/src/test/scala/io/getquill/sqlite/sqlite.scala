package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.ziojdbc.Quill

package object sqlite {
  val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testSqliteDB"))
  object testContext extends Quill.SqliteService(Literal, pool) with TestEntities
}
