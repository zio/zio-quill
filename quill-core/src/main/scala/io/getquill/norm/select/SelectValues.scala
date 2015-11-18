package io.getquill.norm.select

import scala.reflect.macros.whitebox.Context

import io.getquill.ast._

trait SelectValues {
  val c: Context

  sealed trait SelectValue
  case class OptionSelectValue(value: SelectValue) extends SelectValue
  case class SimpleSelectValue(ast: Ast, decoder: c.Tree, optionDecoder: Option[c.Tree]) extends SelectValue
  case class CaseClassSelectValue(tpe: c.Type, params: List[List[SelectValue]]) extends SelectValue
  case class TupleSelectValue(elements: List[SelectValue]) extends SelectValue
}
