package io.getquill.context.sql.idiom

trait QuestionMarkBindVariables { self: SqlIdiom =>

  override def liftingPlaceholder(index: Int): String = s"?"
}
