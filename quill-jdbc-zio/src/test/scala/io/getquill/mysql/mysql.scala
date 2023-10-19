package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.jdbczio.Quill

package object mysql {
  implicit val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testMysqlDB"))
  object testContext extends Quill.Mysql(Literal, pool) with TestEntities
}
