package io.getquill.norm

import io.getquill.Spec
import io.getquill.testContext._

class ConditionalReductionSpec extends Spec {

  "trivial conditionals must" - {
    val q = quote {
      (value: String) =>
        if (value == "foo") qr1.filter(r => r.i == 1)
        else qr1.filter(r => r.s == "blah")
    }

    "reduce to 'then' clause when true" in {
      Normalize(quote(q("foo")).ast) mustEqual (quote(qr1.filter(r => r.i == 1)).ast)
    }

    "reduce to 'else' clause when false" in {
      Normalize(quote(q("bar")).ast) mustEqual (quote(qr1.filter(r => r.s == "blah")).ast)
    }
  }

  "compound condition must" - {
    val q = quote {
      (value: String) =>
        if (value == "foo" || value == "bar") qr1.filter(r => r.i == 1)
        else qr1.filter(r => r.s == "blah")
    }

    "reduce to 'then' clause when true - first" in {
      Normalize(quote(q("foo")).ast) mustEqual (quote(qr1.filter(r => r.i == 1)).ast)
    }

    "reduce to 'then' clause when true - second" in {
      Normalize(quote(q("bar")).ast) mustEqual (quote(qr1.filter(r => r.i == 1)).ast)
    }

    "reduce to 'else' clause when false" in {
      Normalize(quote(q("baz")).ast) mustEqual (quote(qr1.filter(r => r.s == "blah")).ast)
    }
  }

  "recursive compound condition must" - {
    val q = quote {
      (value: String) =>
        if (value == "foo") qr1.filter(r => r.i == 1)
        else if (value == "bar") qr1.filter(r => r.i == 2)
        else qr1.filter(r => r.s == "blah")
    }

    "reduce to 'then' clause when true - first" in {
      Normalize(quote(q("foo")).ast) mustEqual (quote(qr1.filter(r => r.i == 1)).ast)
    }

    "reduce to 'then' clause when true - second" in {
      Normalize(quote(q("bar")).ast) mustEqual (quote(qr1.filter(r => r.i == 2)).ast)
    }

    "reduce to 'else' clause when false" in {
      Normalize(quote(q("baz")).ast) mustEqual (quote(qr1.filter(r => r.s == "blah")).ast)
    }
  }
}
