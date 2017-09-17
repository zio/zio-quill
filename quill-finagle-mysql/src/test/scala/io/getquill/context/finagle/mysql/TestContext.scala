package io.getquill.context.finagle.mysql

import io.getquill.Literal
import io.getquill.TestEntities
import io.getquill.FinagleMysqlContext
import io.getquill.context.sql.{ TestDecoders, TestEncoders }

object testContext extends FinagleMysqlContext(Literal, "testDB") with TestEntities with TestEncoders with TestDecoders
