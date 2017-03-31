package io.getquill.context.sql

import io.getquill._

object testContext extends TestContextTemplate[Literal] {

  def withNaming[Naming <: NamingStrategy] = new TestContextTemplate[Naming]
}

class TestContextTemplate[Naming <: NamingStrategy]
  extends SqlMirrorContext[MirrorSqlDialect, Literal]
  with TestEntities
  with TestEncoders
  with TestDecoders
