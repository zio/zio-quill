package io.getquill.context.jdbc.postgres

import io.getquill.{JdbcContext, Literal, PostgresDialect, TestExtras}


object testContext extends JdbcContext[PostgresDialect, Literal]("testPostgresDB") with TestExtras
