package io.getquill.source.sql

import io.getquill.util.Messages._
import scala.reflect.macros.whitebox.Context
import io.getquill.ast.Ast
import io.getquill.ast.Query
import io.getquill.source.SourceMacro
import io.getquill.util.Messages._
import io.getquill.util.Show.Shower
import scala.util.Try
import scala.util.control.NonFatal
import io.getquill.source.sql.idiom.FallbackDialect
import io.getquill.source.sql.idiom.SqlIdiom
import scala.util.Failure
import scala.util.Success

class SqlSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Try => _, _ }

  override def toExecutionTree(ast: Ast) = {
    val sql = show(ast)
    c.info(sql)
    probe(sql)
    q"$sql"
  }

  private def show(ast: Ast) = {
    val d = dialect
    import d._
    ast match {
      case ast: Query =>
        val sql = SqlQuery(ast)
        VerifySqlQuery(sql).map(_.toString).map(c.fail)
        sql.show
      case ast =>
        ast.show
    }
  }

  private def probe(sql: String) =
    resolveSource[SqlSource[SqlIdiom, Any, Any]].flatMap(_.probe(sql)) match {
      case Failure(e) => c.warn(s"The sql query probing failed. Reason '$e'")
      case Success(v) => v
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
