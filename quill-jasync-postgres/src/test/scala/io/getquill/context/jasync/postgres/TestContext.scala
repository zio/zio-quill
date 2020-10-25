package io.getquill.context.jasync.postgres

import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.{ Literal, PostgresJAsyncContext, TestEntities }

class TestContext extends PostgresJAsyncContext(Literal, "testPostgresDB")
  with TestEntities
  with TestEncoders
  with TestDecoders
