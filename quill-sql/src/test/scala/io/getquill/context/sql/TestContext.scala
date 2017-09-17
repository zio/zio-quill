package io.getquill.context.sql

import io.getquill.{ MirrorSqlDialect, NamingStrategy, SqlMirrorContext, TestEntities, Literal }

class TestContextTemplate[Naming <: NamingStrategy](naming: Naming)
  extends SqlMirrorContext(MirrorSqlDialect, naming)
  with TestEntities
  with TestEncoders
  with TestDecoders {

  def withNaming[N <: NamingStrategy](naming: N)(f: TestContextTemplate[N] => Any): Unit = {
    val ctx = new TestContextTemplate[N](naming)
    f(ctx)
    ctx.close
  }
}

object testContext extends TestContextTemplate(Literal)