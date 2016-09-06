package io.getquill.context.jdbc.postgres

import io.getquill.{ JdbcContext, Literal, PostgresDialect, TestEntities }
import io.getquill.context.sql.TestEncoders
import io.getquill.context.sql.TestDecoders

object testContext extends JdbcContext[PostgresDialect, Literal]("testPostgresDB") with TestEntities with TestEncoders with TestDecoders
