package zio.quill.context.cassandra

import zio.quill._
import zio.quill.ast.Ast
import zio.quill.base.Spec

class ExpandMappedInfixSpec extends Spec {

  import mirrorContext._

  "removes identity map" in {
    val i = quote {
      sql"test".as[Query[Int]]
    }
    val q = quote {
      i.map(x => x)
    }
    ExpandMappedInfixCassandra(q.ast: Ast) mustEqual i.ast
  }

  "expands mapped infix wrapping single query" in {
    val q = quote {
      sql"$qr1 ALLOW FILTERING".as[Query[TestEntity]].map(t => t.i)
    }
    val n = quote {
      sql"${qr1.map(t => t.i)} ALLOW FILTERING".as[Query[TestEntity]]
    }
    ExpandMappedInfixCassandra(q.ast: Ast) mustEqual n.ast
  }

}
