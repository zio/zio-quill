package io.getquill.context.jdbc.sqlite

import io.getquill.context.sql.EncodingSpec

class JdbcEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in pendingUntilFixed {
    testContext.run(delete)
    testContext.run(insert)(insertValues)
    verify(testContext.run(query[EncodingTestEntity]))
  }

  // Remove this workaround once the issue is fixed
  // https://bitbucket.org/xerial/sqlite-jdbc/issues/155/empty-blobs-are-returned-as-null-instead
  "(with workaround) encodes and decodes types" in {
    testContext.run(delete)
    testContext.run(insert)(insertValues)
    val result = testContext.run(query[EncodingTestEntity])
    verify(workaroundSqliteJdbcBug(result))
  }

  private[this] def workaroundSqliteJdbcBug(result: List[EncodingTestEntity]) =
    result.map { e =>
      e.v10 match {
        case null => e.copy(v10 = Array())
        case _    => e
      }
    }
}
