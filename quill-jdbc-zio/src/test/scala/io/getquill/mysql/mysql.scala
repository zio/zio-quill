package io.getquill

import io.getquill.ZioSpec.runLayerUnsafe
import io.getquill.ziojdbc.Quill

package object mysql {
  implicit val pool = runLayerUnsafe(Quill.DataSource.fromPrefix("testMysqlDB"))
  object testContext extends Quill.MysqlService(Literal, pool) with TestEntities
}
