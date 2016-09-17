package io.getquill.context.finagle.postgres

import io.getquill._
import io.getquill.context.sql.{ TestDecoders, TestEncoders }

object testContext extends FinaglePostgresContext[Literal]("testPostgresDB") with TestEntities with TestEncoders with TestDecoders
