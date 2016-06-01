package io.getquill.sources.sql

import scala.reflect.macros.whitebox.Context
import io.getquill.ast._
import io.getquill.naming.NamingStrategy
import io.getquill.sources.SourceMacro
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.util.Messages.RichContext
import io.getquill.sources.sql.idiom.SqlIdiom
import io.getquill.quotation.IsDynamic
import io.getquill.naming.LoadNaming
import io.getquill.util.LoadObject

class SqlSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Try => _, Literal => _, Ident => _, _ }

  override protected def prepare(ast: Ast, params: List[Ident]) = {
    if (!IsDynamic(ast)) {
      implicit val (d, n) = dialectAndNamingStatic
      val (sql, idents, generated) = Prepare(ast, params)
      c.info(sql)
      probe(sql, d)
      q"($sql, $idents, $generated)"
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
    probeQuery[SqlSource[SqlIdiom, NamingStrategy, Any, Any]](_.probe(d.prepare(sql)))

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
