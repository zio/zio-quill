package io.getquill.sources.jdbc

import io.getquill.naming.Literal
import io.getquill.sources.sql.idiom.PostgresDialect

package object postgres {

  val testPostgresDB = new JdbcSource[PostgresDialect, Literal]("testPostgresDB")
}
