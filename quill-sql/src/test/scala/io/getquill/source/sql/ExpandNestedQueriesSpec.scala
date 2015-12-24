package io.getquill.source.sql

import io.getquill._
import io.getquill.norm.Normalize
import io.getquill.source.sql.mirror.mirrorSource

class ExpandNestedQueriesSpec extends Spec {

  "keeps the initial table alias" in {
    val q = quote {
      (for {
        a <- qr1
        b <- qr2
      } yield b).take(10)
    }

    mirrorSource.run(q).sql mustEqual
      "SELECT b.s, b.i, b.l, b.o FROM (SELECT b.s, b.i, b.l, b.o FROM TestEntity a, TestEntity2 b) b LIMIT 10"
  }

  "partial select" in {
    val q = quote {
      (for {
        a <- qr1
        b <- qr2
      } yield b.i).take(10)
    }
    mirrorSource.run(q).sql mustEqual
      "SELECT b.* FROM (SELECT b.i FROM TestEntity a, TestEntity2 b) b LIMIT 10"
  }
}
