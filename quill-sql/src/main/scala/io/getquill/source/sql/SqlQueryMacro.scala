package io.getquill.source.sql

import scala.reflect.macros.whitebox.Context

import AstShow._
import io.getquill.ast.Ast
import io.getquill.source.QueryMacro
import io.getquill.util.Messages.RichContext
import io.getquill.util.Show.Shower

class SqlQueryMacro(val c: Context) extends QueryMacro {

  import c.universe._

  override def toExecutionTree(ast: Ast) = {
    val sql = ast.show
    c.info(sql)
    q"$sql"
  }
}
