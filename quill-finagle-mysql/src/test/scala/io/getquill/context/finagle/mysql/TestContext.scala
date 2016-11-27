package io.getquill.context.finagle.mysql

import io.getquill.{FinagleMysqlContext, Literal, TestExtras}

object testContext extends FinagleMysqlContext[Literal]("testDB") with TestExtras
