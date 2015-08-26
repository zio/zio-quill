package io.getquill.norm.select

import io.getquill.Spec

import language.reflectiveCalls
import io.getquill._
import io.getquill.ast._

class ExtractSelectSpec extends Spec {

  "extracts the final map (select) from a query" - {
    "simple query" in {
      val q = quote {
        qr1.map(t => t.s)
      }
      ExtractSelect(q.ast) match {
        case (query, select) =>
          query mustEqual q.ast
          select mustEqual Property(Ident("t"), "s")
      }
    }
    "nested query" in {
      val q = quote {
        qr1.flatMap(t => qr2.map(u => u.s))
      }
      val m = quote {
        qr2.map(u => u.s)
      }
      ExtractSelect(q.ast) match {
        case (query, select) =>
          query mustEqual q.ast
          select mustEqual Property(Ident("u"), "s")
      }
    }
  }

  "creates a final map (select) if necessary" - {
    "simple query" in {
      val m = quote {
        qr1.map(x => x)
      }
      ExtractSelect(qr1.ast) match {
        case (query, select) =>
          query mustEqual m.ast
          select mustEqual Ident("x")
      }
    }
    "nested query" in {
      val q = quote {
        qr1.flatMap(t => qr2)
      }
      val m = quote {
        qr1.flatMap(t => qr2.map(x => x))
      }
      ExtractSelect(q.ast) match {
        case (query, select) =>
          query mustEqual m.ast
          select mustEqual Ident("x")
      }
    }
    "with filter" in {
      val q = quote {
        qr1.filter(t => t.s == "s1")
      }
      val m = quote {
        qr1.filter(t => t.s == "s1").map(t => t)
      }
      ExtractSelect(q.ast) match {
        case (query, select) =>
          query mustEqual m.ast
          select mustEqual Ident("t")
      }
    }
  }

  "fails if the query is malformed" in {
    intercept[IllegalStateException] {
      ExtractSelect(FlatMap(Ident("a"), Ident("b"), Ident("c")))
    }
  }
}
