package io.getquill.context.sql.idiom

import io.getquill._
import io.getquill.idiom.StringToken

class SQLServerDialectSpec extends Spec {

  "emptySetContainsToken" in {
    SQLServerDialect.emptySetContainsToken(StringToken("w/e")) mustBe StringToken("1 <> 1")
  }

  val ctx = new SqlMirrorContext(SQLServerDialect, Literal) with TestEntities
  import ctx._

  "uses + instead of ||" in {
    val q = quote {
      qr1.map(t => t.s + t.s)
    }
    ctx.run(q).string mustEqual
      "SELECT t.s + t.s FROM TestEntity t"
  }

  "top" in {
    val q = quote {
      qr1.take(15).map(t => t.i)
    }
    ctx.run(q).string mustEqual
      "SELECT TOP 15 t.i FROM TestEntity t"
  }

  "offset/fetch" - {

    val withOrd = quote {
      qr1.sortBy(t => t.i)(Ord.desc).map(_.s)
    }

    def offset[T](q: Quoted[Query[T]]) = quote(q.drop(1))
    def offsetFetch[T](q: Quoted[Query[T]]) = quote(q.drop(2).take(3))

    "offset" in {
      ctx.run(offset(withOrd)).string mustEqual
        "SELECT t.s FROM TestEntity t ORDER BY t.i DESC OFFSET 1 ROWS"
    }

    "offset with fetch " in {
      ctx.run(offsetFetch(withOrd)).string mustEqual
        "SELECT t.s FROM TestEntity t ORDER BY t.i DESC OFFSET 2 ROWS FETCH FIRST 3 ROWS ONLY"
    }

    "fail without ordering" in {
      intercept[IllegalStateException] {
        ctx.run(offset(qr1))
      }.getMessage mustEqual "SQLServer does not support OFFSET without ORDER BY"

      intercept[IllegalStateException] {
        ctx.run(offsetFetch(qr1))
      }.getMessage mustEqual "SQLServer does not support OFFSET without ORDER BY"
    }
  }
}
