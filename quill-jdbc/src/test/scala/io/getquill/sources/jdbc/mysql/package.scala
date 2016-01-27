package io.getquill.sources.jdbc

import io.getquill._
import io.getquill.naming.Literal
import io.getquill.sources.sql.idiom.MySQLDialect

package object mysql {

  val testMysqlDB = source(new JdbcSourceConfig[MySQLDialect, Literal]("testMysqlDB"))
}
