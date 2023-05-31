package io.getquill.context.jdbc.mysql

import io.getquill.context.sql.EncodingSpec
import io.getquill.Query

import java.time.ZoneId

class JdbcEncodingSpec extends EncodingSpec {

  val context = testContext
  import testContext._

  "encodes and decodes types" in {
    testContext.run(delete)
    testContext.run(liftQuery(insertValues).foreach(p => insert(p)))
    verify(testContext.run(query[EncodingTestEntity]))
  }

  "encodes sets" in {
    testContext.run(query[EncodingTestEntity].delete)
    testContext.run(liftQuery(insertValues).foreach(p => query[EncodingTestEntity].insertValue(p)))
    val q = quote { (set: Query[Int]) =>
      query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    verify(testContext.run(q(liftQuery(insertValues.map(_.v6).toSet))))
  }

  "Encode/Decode Other Time Types" in {
    context.run(query[TimeEntity].delete)
    val zid        = ZoneId.systemDefault()
    val timeEntity = TimeEntity.make(zid)
    context.run(query[TimeEntity].insertValue(lift(timeEntity)))
    val actual = context.run(query[TimeEntity]).head
    timeEntity mustEqual actual
  }
}
