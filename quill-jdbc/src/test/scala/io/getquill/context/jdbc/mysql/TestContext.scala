package io.getquill.context.jdbc.mysql

import io.getquill.TestEntities
import io.getquill.naming.Literal
import io.getquill.JdbcContext
import io.getquill.context.sql.idiom.MySQLDialect

object testContext extends JdbcContext[MySQLDialect, Literal]("testMysqlDB") with TestEntities
