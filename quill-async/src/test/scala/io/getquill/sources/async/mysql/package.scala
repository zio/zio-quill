package io.getquill.sources.async

import io.getquill.naming.Literal
import io.getquill._

package object mysql {

  val testMysqlDB = source(new MysqlAsyncSourceConfig[Literal]("testMysqlDB"))

}
