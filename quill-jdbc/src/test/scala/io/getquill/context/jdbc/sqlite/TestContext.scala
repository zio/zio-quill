package io.getquill.context.jdbc.sqlite

import io.getquill.{ JdbcContext, Literal, SqliteDialect, TestEntities }
import io.getquill.context.sql.TestEncoders
import io.getquill.context.sql.TestDecoders

object testContext extends JdbcContext[SqliteDialect, Literal]("testSqliteDB") with TestEntities with TestEncoders with TestDecoders
