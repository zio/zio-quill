package io.getquill.context.jdbc.h2

import io.getquill.{ H2Dialect, JdbcContext, Literal, TestExtras }

object testContext extends JdbcContext[H2Dialect, Literal]("testH2DB") with TestExtras
