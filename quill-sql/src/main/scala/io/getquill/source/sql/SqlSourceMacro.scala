package io.getquill.source.sql

import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Ast
import io.getquill.source.SourceMacro
import io.getquill.util.Messages._
import io.getquill.util.Show.Shower
import scala.util.Try
import scala.util.control.NonFatal
import io.getquill.source.sql.idiom.FallbackDialect
import io.getquill.source.sql.idiom.SqlIdiom

class SqlSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Try => _, _ }

  override def toExecutionTree(ast: Ast) = {
    val d = dialect
    import d._
    val sql = ast.show
    c.info(sql)
    probe(sql)
    q"$sql"
  }

  private def probe(sql: String) =
    try resolveSource[SqlSource[SqlIdiom, Any, Any]].probe(sql).get
    catch {
      case NonFatal(e) => c.warn(s"The sql query probing failed. Reason '$e'")
    }

  private def dialect = {
    val cls =
      c.prefix.actualType
        .baseType(c.weakTypeOf[SqlSource[SqlIdiom, Any, Any]].typeSymbol)
        .typeArgs.head.termSymbol.fullName
    loadClass(cls + "$")
      .map(cls => cls.getField("MODULE$").get(cls)) match {
        case Some(d: SqlIdiom) => d
        case other             => c.fail("Can't load the source's sql dialect.")
      }
  }
}
