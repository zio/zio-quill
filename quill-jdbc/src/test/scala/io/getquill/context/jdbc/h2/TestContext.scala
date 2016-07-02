package io.getquill.context.jdbc.h2

import io.getquill.TestEntities
import io.getquill.naming.Literal
import io.getquill.context.sql.idiom.H2Dialect
import io.getquill.JdbcContext

object testContext extends JdbcContext[H2Dialect, Literal]("testH2DB") with TestEntities
