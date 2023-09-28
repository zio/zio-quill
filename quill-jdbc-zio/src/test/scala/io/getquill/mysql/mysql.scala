package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.jdbczio.Quill
import javax.sql.DataSource

package object mysql {
  implicit val pool: DataSource = runLayerUnsafe(Quill.DataSource.fromPrefix("testMysqlDB"))
  object testContext extends Quill.Mysql(Literal, pool) with TestEntities
}
