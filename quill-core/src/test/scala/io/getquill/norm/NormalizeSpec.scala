package io.getquill.norm

import io.getquill.Spec

class NormalizeSpec extends Spec {

  "normalizes random-generated queries" - {
    val gen = new QueryGenerator(1)
    for (i <- (5 to 20)) {
      val query = gen(i)
      s"$i levels - $query" in {
        val q = VerifyNormalization(Normalize(query))
      }
    }
  }
}
