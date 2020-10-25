package io.getquill

import io.getquill.context.sql.idiom.{ ConcatSupport, QuestionMarkBindVariables, SqlIdiom }
import io.getquill.context._
import io.getquill.sql.idiom.BooleanLiteralSupport

trait MirrorSqlDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with CanReturnField

// TOODO Move these others ones into MirrorSqlDialect main class
trait MirrorSqlDialectWithReturnMulti
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with CanReturnMultiField

trait MirrorSqlDialectWithReturnClause
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with CanReturnClause

trait MirrorSqlDialectWithOutputClause
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with CanOutputClause

trait MirrorSqlDialectWithNoReturn
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with CannotReturn

trait MirrorSqlDialectWithBooleanLiterals
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with CanReturnField
  with BooleanLiteralSupport

object MirrorSqlDialect extends MirrorSqlDialect {

  trait StrategizeElements
    extends SqlIdiom
    with QuestionMarkBindVariables
    with ConcatSupport
    with CanReturnField {

    override def tokenizeIdentName(strategy: NamingStrategy, name: String): String = strategy.default(name)
    override def tokenizeTableAlias(strategy: NamingStrategy, table: String): String = strategy.default(table)
    override def tokenizeColumnAlias(strategy: NamingStrategy, column: String): String = strategy.default(column)
    override def tokenizeFixedColumn(strategy: NamingStrategy, column: String): String = strategy.default(column)
    override def prepareForProbing(string: String) = string
  }
  object StrategizeElements extends StrategizeElements

  override def prepareForProbing(string: String) = string
}

object MirrorSqlDialectWithReturnMulti extends MirrorSqlDialectWithReturnMulti {
  override def prepareForProbing(string: String) = string
}

object MirrorSqlDialectWithReturnClause extends MirrorSqlDialectWithReturnClause {
  override def prepareForProbing(string: String) = string
}

object MirrorSqlDialectWithOutputClause extends MirrorSqlDialectWithOutputClause {
  override def prepareForProbing(string: String) = string
}

object MirrorSqlDialectWithNoReturn extends MirrorSqlDialectWithNoReturn {
  override def prepareForProbing(string: String) = string
}

object MirrorSqlDialectWithBooleanLiterals extends MirrorSqlDialectWithBooleanLiterals {
  override def prepareForProbing(string: String) = string
}
