package io.getquill.context.async.postgres

import io.getquill.{ Literal, PostgresAsyncContext, TestExtras }

class TestContext
  extends PostgresAsyncContext[Literal]("testPostgresDB") with TestExtras
