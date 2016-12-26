package io.getquill.context.jdbc.mysql

import io.getquill.{ JdbcContext, Literal, MySQLDialect, TestExtras }

object testContext extends JdbcContext[MySQLDialect, Literal]("testMysqlDB") with TestExtras
