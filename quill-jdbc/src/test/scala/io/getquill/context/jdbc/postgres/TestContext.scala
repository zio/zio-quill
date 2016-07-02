package io.getquill.context.jdbc.postgres

import io.getquill.TestEntities
import io.getquill.naming.Literal
import io.getquill.JdbcContext
import io.getquill.context.sql.idiom.PostgresDialect

object testContext extends JdbcContext[PostgresDialect, Literal]("testPostgresDB") with TestEntities
