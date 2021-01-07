package io.getquill.context.sql.norm

import io.getquill.Spec
import io.getquill.context.sql.testContext._
import io.getquill.Query

class ExpandMappedInfixSpec extends Spec {
  "expand infix out of map body if first part is empty" in {
    val forUpdate = quote {
      q: Query[TestEntity] => infix"$q FOR UPDATE".as[Query[TestEntity]]
    }
    val q = quote {
      forUpdate(qr1).map(x => x)
    }
    q.ast.toString mustEqual
      s"""infix"$${querySchema("TestEntity")} FOR UPDATE".map(x => x)"""

    ExpandMappedInfix(q.ast).toString mustEqual
      s"""infix"$${querySchema("TestEntity").map(x => x)} FOR UPDATE""""
  }

  "do not expand other cases" in {
    val forUpdate = quote {
      q: Query[TestEntity] => infix"SELECT $q FOR UPDATE".as[Query[TestEntity]]
    }
    val q = quote {
      forUpdate(qr1).map(x => x)
    }
    ExpandMappedInfix(q.ast) mustEqual q.ast
  }

}
