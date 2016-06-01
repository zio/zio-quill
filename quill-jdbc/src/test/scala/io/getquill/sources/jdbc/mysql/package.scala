package io.getquill.sources.jdbc

import io.getquill.naming.Literal
import io.getquill.sources.sql.idiom.MySQLDialect

package object mysql {

  val testMysqlDB = new JdbcSource[MySQLDialect, Literal]("testMysqlDB")
}
