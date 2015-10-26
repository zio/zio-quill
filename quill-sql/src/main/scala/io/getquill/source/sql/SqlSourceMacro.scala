package io.getquill.source.sql

import scala.reflect.macros.whitebox.Context
import scala.util.Failure
import scala.util.Success
import io.getquill.ast._
import io.getquill.source.SourceMacro
import io.getquill.source.sql.idiom.SqlIdiom
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show.Shower
import io.getquill.quotation.Quoted

class SqlSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Try => _, _ }

  override protected def ast[T](quoted: Expr[Quoted[T]]) =
    ExpandOuterJoin(super.ast(quoted))

  override def toExecutionTree(ast: Ast) = {
    val d = dialect
    val sql = show(ast, d)
    c.info(sql)
    probe(sql, d)
    q"$sql"
  }

  private def show(ast: Ast, d: SqlIdiom) = {
    import d._
    ast match {
      case ast: Query =>
        val sql = SqlQuery(ast)
        VerifySqlQuery(sql).map(c.fail)
        sql.show
      case ast =>
        ast.show
    }
  }

  private def probe(sql: String, d: SqlIdiom) =
    resolveSource[SqlSource[SqlIdiom, Any, Any]].map {
      _.probe(d.prepare(sql).getOrElse(sql))
    } match {
      case Some(Failure(e)) => c.error(s"The sql query probing failed. Reason '$e'")
      case other            =>
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
