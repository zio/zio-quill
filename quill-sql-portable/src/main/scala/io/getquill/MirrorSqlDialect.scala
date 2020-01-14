package io.getquill

import io.getquill.context.sql.idiom.{ ConcatSupport, QuestionMarkBindVariables, SqlIdiom }
import io.getquill.context._

trait MirrorSqlDialect
  extends SqlIdiom
  with QuestionMarkBindVariables
  with ConcatSupport
  with CanReturnField

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

object MirrorSqlDialect extends MirrorSqlDialect {
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
