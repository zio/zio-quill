package io.getquill.context.jdbc.sqlite

import io.getquill.TestEntities
import io.getquill.Literal
import io.getquill.JdbcContext
import io.getquill.SqliteDialect

object testContext extends JdbcContext[SqliteDialect, Literal]("testSqliteDB") with TestEntities
