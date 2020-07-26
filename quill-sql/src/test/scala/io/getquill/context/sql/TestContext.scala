package io.getquill.context.sql

import io.getquill
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.norm.EqualityBehavior
import io.getquill.norm.EqualityBehavior.NonAnsiEquality
import io.getquill._

class TestContextTemplate[Dialect <: SqlIdiom, Naming <: NamingStrategy](dialect: Dialect, naming: Naming)
  extends SqlMirrorContext(dialect, naming)
  with TestEntities
  with TestEncoders
  with TestDecoders {

  def withNaming[N <: NamingStrategy](naming: N)(f: TestContextTemplate[Dialect, N] => Any): Unit = {
    val ctx = new TestContextTemplate[Dialect, N](dialect, naming)
    f(ctx)
    ctx.close
  }

  def withDialect[I <: SqlIdiom](dialect: I)(f: TestContextTemplate[I, Naming] => Any): Unit = {
    val ctx = new TestContextTemplate[I, Naming](dialect, naming)
    f(ctx)
    ctx.close
  }
}

trait UpperCaseNonDefault extends NamingStrategy {
  override def column(s: String): String = s.toUpperCase
  override def table(s: String): String = s.toUpperCase
  override def default(s: String) = s
}
object UpperCaseNonDefault extends getquill.UpperCaseNonDefault

object testContext extends TestContextTemplate[MirrorSqlDialect, Literal](MirrorSqlDialect, Literal)
object testContextSnake extends TestContextTemplate[MirrorSqlDialect, SnakeCase](MirrorSqlDialect, SnakeCase)
object testContextUpper extends TestContextTemplate[MirrorSqlDialect, getquill.UpperCaseNonDefault](MirrorSqlDialect, UpperCaseNonDefault)
object testContextEscape extends TestContextTemplate[MirrorSqlDialect, Escape](MirrorSqlDialect, Escape)
object testContextEscapeAndAlias extends TestContextTemplate[MirrorSqlDialect, Escape](MirrorSqlDialect, Escape)
object testContextUpperEscapeColumn extends TestContextTemplate[MirrorSqlDialect, getquill.UpperCaseEscapeColumn](MirrorSqlDialect, UpperCaseEscapeColumn)
object testContextEscapeElements extends TestContextTemplate[MirrorSqlDialect.StrategizeElements, Escape](MirrorSqlDialect.StrategizeElements, Escape)

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
}

object nonAnsiTestContext extends NonAnsiTestContextTemplate(Literal)