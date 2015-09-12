package io.getquill.norm

import io.getquill._

class NormalizeSpec extends Spec {

  "normalizes random-generated queries" - {
    val gen = new QueryGenerator(1)
    for (i <- (3 to 15)) {
      for (j <- (0 until 30)) {
        val query = gen(i)
        val n = Normalize(query)
        if (i == 4 && j == 6) {
          s"$i levels ($j) - $query" in {
            val q = VerifyNormalization(n)
          }
        }
      }
    }
  }

  //  "reduces nested structures" in {
  //    val q = quote {
  //      qr1.flatMap(x => qr2.sortBy(t => t.s).filter(t => t.s == "s1"))
  //    }
  //    val n = quote {
  //      qr1.flatMap(x => qr2.filter(t => t.s == "s1").sortBy(t => t.s))
  //    }
  //    Normalize(q.ast) mustEqual n.ast
  //  }
  //
  //  "applies intermediate map" in {
  //    val q = quote {
  //      qr1.map(b => "s1").map(d => d == "s2")
  //    }
  //    val n = quote {
  //      qr1.map(b => "s1" == "s2")
  //    }
  //    Normalize(q.ast) mustEqual n.ast
  //  }

}
