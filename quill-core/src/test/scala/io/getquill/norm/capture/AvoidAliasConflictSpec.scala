package io.getquill.norm.capture

import scala.language.reflectiveCalls

import io.getquill._

class AvoidAliasConflictSpec extends Spec {

  "renames alias to avoid conflict between queryables during normalization" in {
    val q = quote {
      qr1.filter(u => u.s == "s1").flatMap(u => qr2.flatMap(u => qr3.sortBy(u => u.s).filter(u => u.s == "s1").map(u => u.s)))
    }
    val n = quote {
      qr1.filter(u => u.s == "s1").flatMap(u => qr2.flatMap(u1 => qr3.sortBy(u2 => u2.s).filter(u => u.s == "s1").map(u => u.s)))
    }
    AvoidAliasConflict(q.ast) mustEqual n.ast
  }

  "doesn't change the query if it doesn't have conflicts" in {
    val q = quote {
      qr1.flatMap(a => qr2.sortBy(b => b.s).filter(c => c.s == "s1")).flatMap(d => qr3.map(e => e.s))
    }
    AvoidAliasConflict(q.ast) mustEqual q.ast
  }
}
