package io.getquill.context.sql

import io.getquill.{ MirrorSqlDialect, NamingStrategy, SqlMirrorContext, TestEntities, Literal }

class TestContextTemplate[Naming <: NamingStrategy]
  extends SqlMirrorContext[MirrorSqlDialect, Naming]
  with TestEntities
  with TestEncoders
  with TestDecoders {

  def withNaming[N <: NamingStrategy](f: TestContextTemplate[N] => Any): Unit = {
    val ctx = new TestContextTemplate[N]
    f(ctx)
    ctx.close
  }
}

object testContext extends TestContextTemplate[Literal]