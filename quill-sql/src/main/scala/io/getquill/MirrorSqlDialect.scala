package io.getquill

import io.getquill.context.{ CanReturnClause, CanReturnField, CanReturnMultiField, CannotReturn }
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.context.sql.idiom.QuestionMarkBindVariables
import io.getquill.context.sql.idiom.ConcatSupport

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

object MirrorSqlDialectWithNoReturn extends MirrorSqlDialectWithNoReturn {
  override def prepareForProbing(string: String) = string
}
