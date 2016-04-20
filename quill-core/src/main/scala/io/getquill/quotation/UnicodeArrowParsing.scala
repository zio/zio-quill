package io.getquill.quotation

import scala.reflect.macros.whitebox.Context

trait UnicodeArrowParsing {
  val c: Context

  import c.universe.Quasiquote

  private val arrow = pq"→|->"
}
