package io.getquill.context.async.postgres

import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.{ Literal, PostgresAsyncContext, TestEntities }

class TestContext extends PostgresAsyncContext(Literal, "testPostgresDB")
  with TestEntities
  with TestEncoders
  with TestDecoders
