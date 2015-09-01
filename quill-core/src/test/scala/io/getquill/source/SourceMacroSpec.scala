package io.getquill.source

import io.getquill.Spec
import io.getquill.quotation.Quoted
import io.getquill.quote

class SourceMacroSpec extends Spec {

  "fails if the quotation is not runnable" in {
    val q = quote {
      (s: String) => s
    }
    "mirrorSource.run(q)" mustNot compile
  }

  "fails if unquotation fails" in {
    val q: Quoted[Int] = null
    "mirrorSource.run(q)" mustNot compile
  }
}
