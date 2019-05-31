package io.getquill.context.sql

import io.getquill.norm.EqualityBehavior
import io.getquill.norm.EqualityBehavior.NonAnsiEquality
import io.getquill.{ Literal, MirrorSqlDialect, NamingStrategy, SqlMirrorContext, TestEntities }

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

trait NonAnsiMirrorSqlDialect extends MirrorSqlDialect {
  override def equalityBehavior: EqualityBehavior = NonAnsiEquality
}
object NonAnsiMirrorSqlDialect extends NonAnsiMirrorSqlDialect {
  override def prepareForProbing(string: String) = string
}

class NonAnsiTestContextTemplate[Naming <: NamingStrategy](naming: Naming)
  extends SqlMirrorContext(NonAnsiMirrorSqlDialect, naming)
  with TestEntities
  with TestEncoders
  with TestDecoders {

  def withNaming[N <: NamingStrategy](naming: N)(f: TestContextTemplate[N] => Any): Unit = {
    val ctx = new TestContextTemplate[N](naming)
    f(ctx)
    ctx.close
  }
}

object nonAnsiTestContext extends NonAnsiTestContextTemplate(Literal)