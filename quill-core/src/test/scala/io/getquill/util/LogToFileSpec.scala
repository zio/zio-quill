package io.getquill.util

import io.getquill.testContext._
import io.getquill.{ Spec, testContext }

import scala.io.Source

class LogToFileSpec extends Spec {

  "logs a query to file when enabled" in {
    val q = quote {
      qr1.leftJoin(qr2).on((a, b) => a.i == b.i)
    }
    testContext.run(q)

    val queryFile = Source.fromFile("queries.sql")
    val contents = queryFile.mkString.trim
    queryFile.close()

    contents must not be empty
    contents must endWith("""querySchema("TestEntity").leftJoin(querySchema("TestEntity2")).on((a, b) => a.i == b.i).map(x => (x._1.s, x._1.i, x._1.l, x._1.o, x._1.b, x._2.map((v) => v.s), x._2.map((v) => v.i), x._2.map((v) => v.l), x._2.map((v) => v.o)));""")

  }
}
