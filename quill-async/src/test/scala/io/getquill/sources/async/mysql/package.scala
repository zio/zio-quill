package io.getquill.sources.async

import io.getquill.naming.Literal

package object mysql {

  val testMysqlDB = new MysqlAsyncSource[Literal]("testMysqlDB")

}
