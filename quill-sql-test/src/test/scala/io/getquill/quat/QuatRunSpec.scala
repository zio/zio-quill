package io.getquill.quat

import io.getquill._
import io.getquill.base.Spec

class QuatRunSpec extends Spec {

  val testContext = new SqlMirrorContext(PostgresDialect, Literal)
  import testContext._

  "should refine quats from generic infixes and express during execution" - {
    case class MyPerson(name: String, age: Int)
    val MyPersonQuat = Quat.Product("MyPersonQuat", "name" -> Quat.Value, "age" -> Quat.Value)

    "from extension methods" in {
      implicit class QueryOps[Q <: Query[_]](q: Q) {
        def appendFoo = quote(sql"$q APPEND FOO".pure.as[Q])
      }
      val q = quote(query[MyPerson].appendFoo)
      q.ast.quat mustEqual Quat.Unknown // I.e ReifyLiftings runs RepropagateQuats to take care of this
      val result = testContext.run(q)
      result.info.topLevelQuat mustEqual MyPersonQuat
      result.string mustEqual "SELECT x.name, x.age FROM MyPerson x APPEND FOO"
    }

    "from extension methods - generic marker" in {
      implicit class QueryOps[Q <: Query[_]](q: Q) {
        def appendFoo = quote(sql"$q APPEND FOO".transparent.pure.as[Q])
      }
      val q = quote(query[MyPerson].appendFoo)
      q.ast.quat mustEqual MyPersonQuat // I.e ReifyLiftings runs RepropagateQuats to take care of this
      val result = testContext.run(q)
      result.info.topLevelQuat mustEqual MyPersonQuat
      result.string mustEqual "SELECT x.name, x.age FROM MyPerson x APPEND FOO"
    }

    "should support query-ops function - multiple var" in {
      def appendFooFun[Q <: Query[_]] = quote((q: Q, i: Int) => sql"$q APPEND $i FOO".transparent.pure.as[Q])
      val q                           = quote(appendFooFun(query[MyPerson], 123))
      q.ast.quat mustEqual Quat.Generic // Is it unknown, how should the reducing work from an infix with multiple vars?
      val result = testContext.run(q)
      result.info.topLevelQuat mustEqual MyPersonQuat
      result.string mustEqual "SELECT x.name, x.age FROM MyPerson x APPEND 123 FOO"
    }

    "should support query-ops function - dynamic function" in {
      def appendFooFun[Q <: Query[_]]: Quoted[Q => Q] = quote((q: Q) => sql"$q APPEND FOO".pure.as[Q])
      val q                                           = quote(appendFooFun(query[MyPerson]))
      q.ast.quat mustEqual Quat.Unknown
      testContext.run(q).string mustEqual "SELECT x.name, x.age FROM MyPerson x APPEND FOO"
    }
  }
}
