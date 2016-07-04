package io.getquill.context.jdbc.postgres

import io.getquill.TestEntities
import io.getquill.Literal
import io.getquill.JdbcContext
import io.getquill.PostgresDialect

object testContext extends JdbcContext[PostgresDialect, Literal]("testPostgresDB") with TestEntities
