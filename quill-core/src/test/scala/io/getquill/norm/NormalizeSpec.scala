package io.getquill.norm

import io.getquill._

class NormalizeSpec extends Spec {

  "normalizes random-generated queries" - {
    val gen = new QueryGenerator(1)
    for (i <- (3 to 15)) {
      for (j <- (0 until 30)) {
        val query = gen(i)
        s"$i levels ($j) - $query" in {
          val n = Normalize(query)
        }
      }
    }
  }

  "doesn't apply the avoid capture normalization to branches in isolation" in {
    val q = quote {
      qr1.sortBy(t => t.i).flatMap(f => qr2.map(t => 1))
    }
    val n = quote {
      qr1.sortBy(t => t.i).flatMap(t => qr2.map(t1 => 1))
    }
    Normalize(q.ast) mustEqual n.ast
  }
}
