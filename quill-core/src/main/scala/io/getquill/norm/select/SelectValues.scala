package io.getquill.norm.select

import scala.reflect.macros.whitebox.Context

import io.getquill.ast.Ast

trait SelectValues {
  val c: Context

  sealed trait SelectValue
  case class SimpleSelectValue(ast: Ast, decoder: c.Tree) extends SelectValue
  case class CaseClassSelectValue(tpe: c.Type, params: List[List[SimpleSelectValue]]) extends SelectValue
}
