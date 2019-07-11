package io.getquill.context.jdbc.oracle

import io.getquill.context.sql.{ EncodingSpec, EncodingTestType }

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
    testContext.run(liftQuery(insertValues).foreach(p => query[EncodingTestEntity].insert(p)))
    val q = quote {
      (set: Query[Int]) =>
        query[EncodingTestEntity].filter(t => set.contains(t.v6))
    }
    verify(testContext.run(q(liftQuery(insertValues.map(_.v6).toSet))))
  }

  def emptyAsNull(e: EncodingTestType) = e.copy(value = if (e.value == "") null else e.value)
  def emptyAsNull(str: String) = if (str == "") null else str
  def emptyAsNull(bytes: Array[Byte]) = if (bytes == null || bytes.isEmpty) null else bytes

  /**
   * Since oracle encodes "" as null, need to modify verification in order to track this
   */
  override def verify(result: List[EncodingTestEntity]) = {
    result.size mustEqual insertValues.size
    result.zip(insertValues).foreach {
      case (e1, e2) =>
        emptyAsNull(e2.v1) mustEqual emptyAsNull(e2.v1)
        e1.v2 mustEqual e2.v2
        e1.v3 mustEqual e2.v3
        e1.v4 mustEqual e2.v4
        e1.v5 mustEqual e2.v5
        e1.v6 mustEqual e2.v6
        e1.v7 mustEqual e2.v7
        e1.v8 mustEqual e2.v8
        e1.v9 mustEqual e2.v9
        emptyAsNull(e1.v10) mustEqual emptyAsNull(e2.v10)
        e1.v11 mustEqual e2.v11
        emptyAsNull(e1.v12) mustEqual emptyAsNull(e2.v12)
        e1.v13 mustEqual e2.v13
        e1.v14 mustEqual e2.v14

        e1.o1 mustEqual e2.o1
        e1.o2 mustEqual e2.o2
        e1.o3 mustEqual e2.o3
        e1.o4 mustEqual e2.o4
        e1.o5 mustEqual e2.o5
        e1.o6 mustEqual e2.o6
        e1.o7 mustEqual e2.o7
        e1.o8 mustEqual e2.o8
        e1.o9 mustEqual e2.o9
        e1.o10.getOrElse(Array()) mustEqual e2.o10.getOrElse(Array())
        e1.o11 mustEqual e2.o11
        e1.o12 mustEqual e2.o12
        e1.o13 mustEqual e2.o13
        e1.o14 mustEqual e2.o14
        e1.o15 mustEqual e2.o15
    }
  }
}
