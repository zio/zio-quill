package io.getquill.context.jdbc.mysql

import io.getquill.TestEntities
import io.getquill.Literal
import io.getquill.JdbcContext
import io.getquill.MySQLDialect

object testContext extends JdbcContext[MySQLDialect, Literal]("testMysqlDB") with TestEntities
