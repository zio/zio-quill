package io.getquill.context.jdbc.h2

import io.getquill.TestEntities
import io.getquill.Literal
import io.getquill.H2Dialect
import io.getquill.JdbcContext

object testContext extends JdbcContext[H2Dialect, Literal]("testH2DB") with TestEntities
