package io.getquill.sources.sql

import io.getquill._

class VerifySqlQuerySpec extends Spec {

  "fails if the query can't be translated to applicative joins" - {
    "sortBy" in {
      val q = quote {
        qr1.flatMap(a => qr2.filter(b => b.s == a.s).sortBy(b => b.s).map(b => b.s))
      }
      VerifySqlQuery(SqlQuery(q.ast)).toString mustEqual
        "Some(The monad composition can't be expressed using applicative joins. Faulty expression: 'b.s == a.s'. Free variables: 'List(a)'.)"
    }

    "take" in {
      val q = quote {
        qr1.flatMap(a => qr2.filter(b => b.s == a.s).take(10).map(b => b.s))
      }
      VerifySqlQuery(SqlQuery(q.ast)).toString mustEqual
        "Some(The monad composition can't be expressed using applicative joins. Faulty expression: 'b.s == a.s'. Free variables: 'List(a)'.)"
    }
  }
}
