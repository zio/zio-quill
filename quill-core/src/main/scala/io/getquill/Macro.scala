package io.getquill

import scala.reflect.macros.whitebox.Context

import io.getquill.quotation.Quotation

private[getquill] class Macro(val c: Context) extends Quotation
