package io.getquill

import scala.reflect.macros.whitebox.Context
import io.getquill.quotation.Quotation
import io.getquill.impl.Parser

private[getquill] class Macro(val c: Context) extends Quotation with Parser