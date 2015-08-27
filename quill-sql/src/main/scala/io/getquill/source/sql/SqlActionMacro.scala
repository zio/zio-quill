package io.getquill.source.sql

import scala.reflect.macros.whitebox.Context

import AstShow._
import io.getquill.ast.Ast
import io.getquill.source.ActionMacro
import io.getquill.util.Messages._
import io.getquill.util.Show._

class SqlActionMacro(val c: Context) extends ActionMacro {
  import c.universe._

  override def toExecutionTree(ast: Ast) = {
    val sql = ast.show
    c.info(sql)
    q"$sql"
  }
}
