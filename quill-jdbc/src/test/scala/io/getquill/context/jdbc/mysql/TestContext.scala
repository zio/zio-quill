package io.getquill.context.jdbc.mysql

import io.getquill.context.sql.TestEncoders
import io.getquill.context.sql.TestDecoders
import io.getquill.{ JdbcContext, Literal, MySQLDialect, TestEntities }

object testContext extends JdbcContext[MySQLDialect, Literal]("testMysqlDB") with TestEntities with TestEncoders with TestDecoders
