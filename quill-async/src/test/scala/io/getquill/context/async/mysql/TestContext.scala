package io.getquill.context.async.mysql

import io.getquill.MysqlAsyncContext
import io.getquill.TestEntities
import io.getquill.Literal

object testContext extends MysqlAsyncContext[Literal]("testMysqlDB") with TestEntities
