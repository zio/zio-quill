package io.getquill.context.jdbc.h2

import io.getquill.{ H2Dialect, JdbcContext, Literal, TestEntities }
import io.getquill.context.sql.TestEncoders
import io.getquill.context.sql.TestDecoders

object testContext extends JdbcContext[H2Dialect, Literal]("testH2DB") with TestEntities with TestEncoders with TestDecoders
