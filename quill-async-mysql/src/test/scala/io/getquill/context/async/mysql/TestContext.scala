package io.getquill.context.async.mysql

import io.getquill._

class TestContext extends MysqlAsyncContext[Literal]("testMysqlDB") with TestExtras
