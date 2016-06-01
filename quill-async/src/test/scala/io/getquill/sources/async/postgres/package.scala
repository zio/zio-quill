package io.getquill.sources.async

import io.getquill.naming.Literal

package object postgres {

  val testPostgresDB = new PostgresAsyncSource[Literal]("testPostgresDB")
}
