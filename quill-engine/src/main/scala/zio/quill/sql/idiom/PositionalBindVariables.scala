package zio.quill.context.sql.idiom

trait PositionalBindVariables { self: SqlIdiom =>

  override def liftingPlaceholder(index: Int): String = s"$$${index + 1}"
}
