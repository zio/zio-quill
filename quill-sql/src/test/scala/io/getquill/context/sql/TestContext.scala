package io.getquill.context.sql

import io.getquill._

object testContext extends TestContextTemplate

class TestContextTemplate
  extends SqlMirrorContext[MirrorSqlDialect, Literal]
  with TestEntities
  with TestEncoders
  with TestDecoders
