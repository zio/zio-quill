package io.getquill.sources.finagle

import io.getquill._
import io.getquill.naming.Literal

package object mysql {

  val testDB = source(new io.getquill.FinagleMysqlSourceConfig[Literal]("testDB"))

}
