package io.getquill.source.sql

import scala.reflect.macros.whitebox.Context
import scala.util.Failure
import scala.util.Success

import io.getquill.ast._
import io.getquill.source.SourceMacro
import io.getquill.source.sql.idiom.SqlIdiom
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show.Shower

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
