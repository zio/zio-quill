package io.getquill.context.async.postgres

import io.getquill.PostgresAsyncContext
import io.getquill.TestEntities
import io.getquill.Literal
import io.getquill.context.sql.{ TestDecoders, TestEncoders }

object testContext extends PostgresAsyncContext[Literal]("testPostgresDB") with TestEntities with TestEncoders with TestDecoders
