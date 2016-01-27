package io.getquill.sources.sql

import scala.reflect.macros.whitebox.Context
import scala.util.Failure
import scala.util.Success
import io.getquill.ast._
import io.getquill.naming.NamingStrategy
import io.getquill.sources.SourceMacro
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show.Shower
import io.getquill.quotation.Quoted
import scala.reflect.ClassTag
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.norm.Normalize
import io.getquill.quotation.IsDynamic
import io.getquill.naming.LoadNaming
import io.getquill.util.LoadObject

class SqlSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Try => _, Literal => _, Ident => _, _ }

  override protected def prepare(ast: Ast, params: List[Ident]) = {
    if (!IsDynamic(ast)) {
      implicit val (d, n) = dialectAndNamingStatic
      val (sql, idents) = Prepare(ast, params)
      c.info(sql)
      probe(sql, d)
      q"($sql, $idents)"
    } else {
      c.info("Dynamic query")
      q"""
      {
        implicit val (d, n) = $dialectAndNamingDynamic
        io.getquill.sources.sql.Prepare($ast, $params)
      }
      """
    }
  }

  private def probe(sql: String, d: SqlIdiom) =
    resolveSource[SqlSource[SqlIdiom, NamingStrategy, Any, Any]].map {
      _.probe(d.prepare(sql))
    } match {
      case Some(Failure(e)) => c.error(s"The sql query probing failed. Reason '$e'")
      case other            =>
    }

  private def dialectAndNaming = {
    val (idiom :: n :: _) =
      c.prefix.actualType
        .baseType(c.weakTypeOf[SqlSource[SqlIdiom, NamingStrategy, Any, Any]].typeSymbol)
        .typeArgs
    (idiom, n)
  }

  private def dialectAndNamingDynamic = {
    val (idiom, naming) = dialectAndNaming
    q"(${idiom.typeSymbol.companion}, ${LoadNaming.dynamic(c)(naming)})"
  }

  private def dialectAndNamingStatic = {
    val (idiom, naming) = dialectAndNaming
    (LoadObject[SqlIdiom](c)(idiom), LoadNaming.static(c)(naming))
  }
}
