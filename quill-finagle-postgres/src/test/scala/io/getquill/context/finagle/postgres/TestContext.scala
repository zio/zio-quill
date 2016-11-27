package io.getquill.context.finagle.postgres

import io.getquill.{FinaglePostgresContext, Literal, TestExtras}

object testContext extends FinaglePostgresContext[Literal]("testPostgresDB") with TestExtras
