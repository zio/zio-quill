package io.getquill.source.sql

import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Ast
import io.getquill.source.SourceMacro
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show.Shower
import io.getquill.source.sql.idiom.StandardSqlDialect
import scala.util.Try

class SqlSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Try => _, _ }

  override def toExecutionTree(ast: Ast) = {
    val dialect = Try(self[SqlSource[_, _]].map(_.dialect)).toOption.flatten.getOrElse(StandardSqlDialect)
    import dialect._
    val sql = ast.show
    c.info(sql)
    q"$sql"
  }
}
