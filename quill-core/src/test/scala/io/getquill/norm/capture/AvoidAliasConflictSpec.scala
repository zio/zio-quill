package io.getquill.norm.capture

import io.getquill._
import test.Spec
import language.reflectiveCalls

class AvoidAliasConflictSpec extends Spec {

  "renames alias to avoid conflict between queryables during normalization" - {
    val q = quote {
      qr1.flatMap(u => qr2.filter(u => u.s == "s1")).flatMap(u => qr3.map(u => u.s))
    }
    val n = quote {
      qr1.flatMap(u => qr2.filter(u1 => u1.s == "s1")).flatMap(u => qr3.map(u2 => u2.s))
    }
    AvoidAliasConflict(q.ast) mustEqual n.ast
  }
}
