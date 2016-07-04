package io.getquill.context.finagle.mysql

import io.getquill.Literal
import io.getquill.TestEntities
import io.getquill.FinagleMysqlContext

object testContext extends FinagleMysqlContext[Literal]("testDB") with TestEntities
