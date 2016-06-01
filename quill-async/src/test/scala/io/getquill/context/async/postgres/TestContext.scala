package io.getquill.context.async.postgres

import io.getquill.PostgresAsyncContext
import io.getquill.TestEntities
import io.getquill.Literal

object testContext extends PostgresAsyncContext[Literal]("testPostgresDB") with TestEntities
