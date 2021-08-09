package io.getquill.context.jasync.mysql

import io.getquill.{ Literal, MysqlJAsyncContext, TestEntities }
import io.getquill.context.sql.{ TestDecoders, TestEncoders }

class TestContext extends MysqlJAsyncContext(Literal, "testMysqlDB") with TestEntities with TestEncoders with TestDecoders
