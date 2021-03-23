package io.getquill.context.jasync.qzio.postgres

import io.getquill.context.sql.{ TestDecoders, TestEncoders }
import io.getquill.{ Literal, PostgresJAsyncZioContext, TestEntities }

class TestContext extends PostgresJAsyncZioContext(Literal)
  with TestEntities
  with TestEncoders
  with TestDecoders
