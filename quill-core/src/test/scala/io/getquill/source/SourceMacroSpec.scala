package io.getquill.source

import io.getquill._
import io.getquill.source.mirror.mirrorSource
import io.getquill.quotation.Quoted

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
