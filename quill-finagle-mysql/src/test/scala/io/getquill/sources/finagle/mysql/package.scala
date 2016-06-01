package io.getquill.sources.finagle

import io.getquill.naming.Literal

package object mysql {

  val testDB = new FinagleMysqlSource[Literal]("testDB")

}
