package io.getquill.norm.select

import io.getquill._
import io.getquill.ast._

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
    "aggregated query" in {
      val q = quote {
        qr1.map(t => t.l).min
      }
      ReplaceSelect(q.ast, List(Property(Ident("t"), "l"))) mustEqual
        q.ast
    }
    "doesn't change the select if not necessary" in {
      val q = quote {
        qr1.map(t => t)
      }
      ReplaceSelect(q.ast, List(Ident("t"))) mustEqual q.ast
    }

    "distinct query" in {
      val q = quote {
        qr1.map(t => t.l).distinct
      }
      ReplaceSelect(q.ast, List(Property(Ident("t"), "l"))) mustEqual
        q.ast
    }
  }

  "fails if the query doesn't have a final map (select)" - {
    "simple query" in {
      val q = quote {
        qr1.filter(t => t.s == "s1")
      }
      val e = intercept[IllegalStateException] {
        ReplaceSelect(q.ast, Nil)
      }
    }
    "nested query" in {
      val q = quote {
        qr1.flatMap(u => qr2)
      }
      val e = intercept[IllegalStateException] {
        ReplaceSelect(q.ast, Nil)
      }
    }
  }
}
