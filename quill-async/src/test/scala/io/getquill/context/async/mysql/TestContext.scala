package io.getquill.context.async.mysql

import io.getquill.{ Literal, MysqlAsyncContext, TestEntities }
import io.getquill.context.sql.{ TestDecoders, TestEncoders }

object testContext extends MysqlAsyncContext[Literal]("testMysqlDB") with TestEntities with TestEncoders with TestDecoders
