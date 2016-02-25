package io.getquill.sources.cassandra

import io.getquill._

class CassandraSyncSourceSpec extends Spec {

  "run non-batched action" - {
    val insert = quote { (i: Int) =>
      qr1.insert(_.i -> i)
    }
    testSyncDB.run(insert)(1).all().size mustEqual (1)
  }
}
