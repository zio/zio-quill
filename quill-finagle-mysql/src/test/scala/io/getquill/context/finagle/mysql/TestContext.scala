package io.getquill.context.finagle.mysql

import io.getquill.naming.Literal
import io.getquill.TestEntities

object testContext extends FinagleMysqlContext[Literal]("testDB") with TestEntities
