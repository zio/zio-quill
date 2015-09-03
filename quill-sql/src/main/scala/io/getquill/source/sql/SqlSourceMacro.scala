package io.getquill.source.sql

import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Ast
import io.getquill.source.SourceMacro
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show.Shower
import scala.util.Try
import scala.util.control.NonFatal
import io.getquill.source.sql.idiom.FallbackDialect

class SqlSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Try => _, _ }

  override def toExecutionTree(ast: Ast) = {
    import dialect._
    val sql = ast.show
    c.info(sql)
    q"$sql"
  }

  private lazy val dialect =
    try resolveRequiredSource[SqlSource[_, _]].dialect
    catch {
      case NonFatal(e) =>
        c.warn(s"Can't determine the sql dialect, falling back to a standard dialect. Reason: $e")
        FallbackDialect
    }

}
