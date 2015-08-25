package io.getquill.norm.select

import io.getquill._
import io.getquill.ast._
import test.Spec
import language.reflectiveCalls

class ReplaceSelectSpec extends Spec {

  "replaces the final map (select) body" - {
    "simple query" in {
      val q = quote {
        qr1.map(t => t)
      }
      val n = quote {
        qr1.map(t => (t, t.s))
      }
      ReplaceSelect(q.ast, List(Ident("t"), Property(Ident("t"), "s"))) mustEqual
        n.ast
    }
    "nested query" in {
      val q = quote {
        qr1.flatMap(u => qr2.map(t => t.s))
      }
      val n = quote {
        qr1.flatMap(u => qr2.map(t => (t, t.s)))
      }
      ReplaceSelect(q.ast, List(Ident("t"), Property(Ident("t"), "s"))) mustEqual
        n.ast
    }
  }

  "fails if the query doesn't have a final map (select)" - {
    "simple query" in {
      val q = quote {
        qr1.filter(t => t.s == "s1")
      }
      intercept[IllegalStateException] {
        ReplaceSelect(q.ast, List())
      }
    }
    "nested query" in {
      val q = quote {
        qr1.flatMap(u => qr2)
      }
      intercept[IllegalStateException] {
        ReplaceSelect(q.ast, List())
      }
    }
  }
}
