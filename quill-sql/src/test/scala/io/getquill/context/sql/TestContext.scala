package io.getquill.context.sql

import io.getquill._

object testContext extends TestContextTemplate[Literal] {

  def withNaming[N <: NamingStrategy](f: TestContextTemplate[N] => Any): Unit = {
    val ctx = new TestContextTemplate[N]
    f(ctx)
    ctx.close
  }
}

class TestContextTemplate[N <: NamingStrategy]
  extends SqlMirrorContext[MirrorSqlDialect, N]
  with TestEntities
  with TestEncoders
  with TestDecoders
