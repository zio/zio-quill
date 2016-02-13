package io.getquill.sources.jdbc

import io.getquill._
import io.getquill.naming.Literal
import io.getquill.sources.sql.idiom.PostgresDialect

package object postgres {
  
  val testPostgresDB = source(new JdbcSourceConfig[PostgresDialect, Literal]("testPostgresDB"))
}
