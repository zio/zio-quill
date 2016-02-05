package io.getquill.sources.jdbc.mysql

import io.getquill._
import io.getquill.sources.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  "encodes and decodes types" in {
    testMysqlDB.run(delete)
    testMysqlDB.run(insert)(insertValues)
    verify(testMysqlDB.run(query[EncodingTestEntity]))
  }

  "encodes sets" in {
    testMysqlDB.run(query[EncodingTestEntity].delete)
    testMysqlDB.run(query[EncodingTestEntity].insert)(insertValues)
    val q = quote {
      (set: Set[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    verify(testMysqlDB.run(q)(insertValues.map(_.v6).toSet))
  }
}
