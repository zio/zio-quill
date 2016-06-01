package io.getquill.sources.jdbc

import io.getquill.naming.Literal
import io.getquill.sources.sql.idiom.H2Dialect

package object h2 {

  val testH2DB = new JdbcSource[H2Dialect, Literal]("testH2DB")
}
