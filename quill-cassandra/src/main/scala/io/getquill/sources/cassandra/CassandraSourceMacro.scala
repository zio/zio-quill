package io.getquill.sources.cassandra

import scala.reflect.macros.whitebox.Context

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Row

import io.getquill.ast._
import io.getquill.naming.LoadNaming
import io.getquill.naming.NamingStrategy
import io.getquill.quotation.IsDynamic
import io.getquill.sources.SourceMacro
import io.getquill.util.Messages.RichContext

class CassandraSourceMacro(val c: Context) extends SourceMacro {
  import c.universe.{ Ident => _, _ }

  override protected def prepare(ast: Ast, params: List[Ident]) =
    if (!IsDynamic(ast)) {
      implicit val n = LoadNaming.static(c)(namingType)
      val (cql, idents, _) = Prepare(ast, params)
      c.info(cql)
      probe(cql)
      q"($cql, $idents, None)"
    } else {
      c.info("Dynamic query")
      q"""
      {
        implicit val n = ${LoadNaming.dynamic(c)(namingType)}
        io.getquill.sources.cassandra.Prepare($ast, $params)
      }
      """
    }

  private def probe(cql: String) =
    probeQuery[CassandraSource[NamingStrategy, Row, BoundStatement]](_.probe(cql))

  private def namingType =
    c.prefix.actualType
      .baseType(c.weakTypeOf[CassandraSource[NamingStrategy, Row, BoundStatement]].typeSymbol)
      .typeArgs.head
}
