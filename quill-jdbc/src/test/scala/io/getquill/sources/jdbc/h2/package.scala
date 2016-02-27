package io.getquill.sources.jdbc

import io.getquill._
import io.getquill.naming.Literal
import io.getquill.sources.sql.idiom.H2Dialect

package object h2 {

  val testH2DB = source(new JdbcSourceConfig[H2Dialect, Literal]("testH2DB"))
  val testH2DBWithQueryProbing = source(new JdbcSourceConfig[H2Dialect, Literal]("testH2DB") with QueryProbing)
}
