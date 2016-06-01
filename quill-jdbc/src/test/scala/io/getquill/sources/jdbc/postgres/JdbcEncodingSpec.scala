package io.getquill.sources.jdbc.postgres

import io.getquill.sources.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec(testPostgresDB) {

  import testPostgresDB._

  "encodes and decodes types" in {
    testPostgresDB.run(delete)
    testPostgresDB.run(insert)(insertValues)
    verify(testPostgresDB.run(query[EncodingTestEntity]))
  }

  "encodes sets" in {
    testPostgresDB.run(query[EncodingTestEntity].delete)
    testPostgresDB.run(query[EncodingTestEntity].insert)(insertValues)
    val q = quote {
      (set: Set[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    verify(testPostgresDB.run(q)(insertValues.map(_.v6).toSet))
  }
}
