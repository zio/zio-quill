package io.getquill.context.sql

import io.getquill.TestEntities
import io.getquill.Literal
import io.getquill.MirrorSqlDialect
import io.getquill.SqlMirrorContext

object testContext extends TestContextTemplate

class TestContextTemplate
  extends SqlMirrorContext[MirrorSqlDialect, Literal]
  with TestEntities
  with TestEncoders
  with TestDecoders
