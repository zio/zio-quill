package io.getquill

import scala.reflect.macros.whitebox.Context
import io.getquill.quotation.Quotation
import io.getquill.sources.ResolveSourceMacro

private[getquill] class Macro(val c: Context) extends Quotation with ResolveSourceMacro
