package io.getquill.quotation

trait UnicodeArrowParsing {
  this: Quotation =>

  import c.universe.Quasiquote

  private val arrow = pq"→|->"
}
