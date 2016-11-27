package io.getquill.context.jdbc.sqlite

import io.getquill.{JdbcContext, Literal, SqliteDialect, TestExtras}

object testContext extends JdbcContext[SqliteDialect, Literal]("testSqliteDB") with TestExtras
