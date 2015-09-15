package io.getquill.norm

import io.getquill._

class NormalizeSpec extends Spec {

  "normalizes random-generated queries" - {
    val gen = new QueryGenerator(1)
    for (i <- (3 to 15)) {
      for (j <- (0 until 30)) {
        val query = gen(i)
        val n = Normalize(query)
        s"$i levels ($j) - $query" in {
          val q = VerifyNormalization(n)
        }
      }
    }
  }
}
