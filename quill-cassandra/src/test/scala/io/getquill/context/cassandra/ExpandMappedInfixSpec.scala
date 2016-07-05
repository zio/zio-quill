package io.getquill.context.cassandra

import io.getquill._
import io.getquill.ast.Ast

class ExpandMappedInfixSpec extends Spec {

  import mirrorContext._

  "removes identity map" in {
    val i = quote {
      infix"test".as[Query[Int]]
    }
    val q = quote {
      i.map(x => x)
    }
    ExpandMappedInfix(q.ast: Ast) mustEqual i.ast
  }

  "expands mapped infix wrapping single query" in {
    val q = quote {
      infix"$qr1 ALLOW FILTERING".as[Query[TestEntity]].map(t => t.i)
    }
    val n = quote {
      infix"${qr1.map(t => t.i)} ALLOW FILTERING".as[Query[TestEntity]]
    }
    ExpandMappedInfix(q.ast: Ast) mustEqual n.ast
  }

}
