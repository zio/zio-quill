package io.getquill.sources.async

import io.getquill.naming.Literal
import io.getquill._

package object postgres {

  val testPostgresDB = source(new PostgresAsyncSourceConfig[Literal]("testPostgresDB"))
}
