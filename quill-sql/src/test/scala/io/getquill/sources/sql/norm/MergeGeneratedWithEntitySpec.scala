package io.getquill.sources.sql.norm

import io.getquill._
import io.getquill.Spec
import io.getquill.ast.Entity

class MergeGeneratedWithEntitySpec extends Spec {
  "merge generated" in {
    val q = quote {
      qr1.generated(x => x.i)
    }

    val ast = MergeGeneratedWithEntity(q.ast)
    ast mustEqual
      Entity("TestEntity", generated = Some("i"))
  }
}
