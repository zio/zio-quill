package io.getquill.context.cassandra

import scala.reflect.macros.whitebox.Context

import com.datastax.driver.core.BoundStatement
import com.datastax.driver.core.Row

import io.getquill.ast._
import io.getquill.LoadNaming
import io.getquill.NamingStrategy
import io.getquill.quotation.IsDynamic
import io.getquill.context.ContextMacro
import io.getquill.util.Messages.RichContext

class CassandraContextMacro(val c: Context) extends ContextMacro {
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
        io.getquill.context.cassandra.Prepare($ast, $params)
      }
      """
    }

  private def probe(cql: String) =
    probeQuery[CassandraContext[NamingStrategy, Row, BoundStatement]](_.probe(cql))

  private def namingType =
    c.prefix.actualType
      .baseType(c.weakTypeOf[CassandraContext[NamingStrategy, Row, BoundStatement]].typeSymbol)
      .typeArgs.head
}
