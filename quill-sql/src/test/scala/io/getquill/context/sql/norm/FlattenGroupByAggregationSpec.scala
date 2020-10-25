package io.getquill.context.sql.norm

import io.getquill.ast._
import io.getquill.context.sql.testContext._
import io.getquill.{ Query, Spec }

class FlattenGroupByAggregationSpec extends Spec {

  "flattens mapped aggregation" - {
    "simple" in {
      val q = quote {
        (e: Query[TestEntity]) =>
          e.map(_.i).max
      }
      FlattenGroupByAggregation(Ident("e", TestEntityQuat))(q.ast.body) mustEqual
        Aggregation(AggregationOperator.max, Property(Ident("e"), "i"))
    }
    "nested infix" in {
      val q = quote {
        (e: Query[TestEntity]) =>
          infix"GROUP_CONCAT(${e.map(_.i)})".as[String]
      }
      val n = quote {
        (e: TestEntity) =>
          infix"GROUP_CONCAT(${e.i})".as[String]
      }
      FlattenGroupByAggregation(Ident("e", TestEntityQuat))(q.ast.body) mustEqual n.ast.body
    }
  }

  "doesn't change simple aggregation" in {
    val q = quote {
      (e: Query[TestEntity]) =>
        e.size
    }
    FlattenGroupByAggregation(Ident("e"))(q.ast.body) mustEqual
      q.ast.body
  }

  "doesn't fail for nested query" in {
    val q = quote {
      (e: Query[TestEntity]) =>
        qr2.size
    }
    FlattenGroupByAggregation(Ident("e"))(q.ast.body) mustEqual
      q.ast.body
  }

  "fails for invalid aggregation" - {
    "map" in {
      val q = quote {
        (e: Query[TestEntity]) =>
          e.filter(_.i == 1).map(_.i).max
      }
      val e = intercept[IllegalStateException] {
        FlattenGroupByAggregation(Ident("e"))(q.ast.body)
      }
    }
    "flatMap" in {
      val q = quote {
        (e: Query[TestEntity]) =>
          e.flatMap(_ => qr2).size
      }
      val e = intercept[IllegalStateException] {
        FlattenGroupByAggregation(Ident("e"))(q.ast.body)
      }
    }
    "filter" in {
      val q = quote {
        (e: Query[TestEntity]) =>
          e.filter(_.i == 1).size
      }
      val e = intercept[IllegalStateException] {
        FlattenGroupByAggregation(Ident("e"))(q.ast.body)
      }
    }
    "sortBy" in {
      val q = quote {
        (e: Query[TestEntity]) =>
          e.sortBy(_.i).size
      }
      val e = intercept[IllegalStateException] {
        FlattenGroupByAggregation(Ident("e"))(q.ast.body)
      }
    }
    "take" in {
      val q = quote {
        (e: Query[TestEntity]) =>
          e.take(1).size
      }
      val e = intercept[IllegalStateException] {
        FlattenGroupByAggregation(Ident("e"))(q.ast.body)
      }
    }
    "drop" in {
      val q = quote {
        (e: Query[TestEntity]) =>
          e.drop(1).size
      }
      val e = intercept[IllegalStateException] {
        FlattenGroupByAggregation(Ident("e"))(q.ast.body)
      }
    }
    "union" - {
      "left" in {
        val q = quote {
          (e: Query[TestEntity]) =>
            (e ++ qr1).size
        }
        val e = intercept[IllegalStateException] {
          FlattenGroupByAggregation(Ident("e"))(q.ast.body)
        }
      }
      "right" in {
        val q = quote {
          (e: Query[TestEntity]) =>
            (qr1 ++ e).size
        }
        val e = intercept[IllegalStateException] {
          FlattenGroupByAggregation(Ident("e"))(q.ast.body)
        }
      }
    }
    "unionAll" - {
      "left" in {
        val q = quote {
          (e: Query[TestEntity]) =>
            e.unionAll(qr1).size
        }
        val e = intercept[IllegalStateException] {
          FlattenGroupByAggregation(Ident("e"))(q.ast.body)
        }
      }
      "right" in {
        val q = quote {
          (e: Query[TestEntity]) =>
            qr1.unionAll(e).size
        }
        val e = intercept[IllegalStateException] {
          FlattenGroupByAggregation(Ident("e"))(q.ast.body)
        }
      }
    }
    "outerJoin" - {
      "left" in {
        val q = quote {
          (e: Query[TestEntity]) =>
            e.leftJoin(qr1).on((a, b) => true).size
        }
        val e = intercept[IllegalStateException] {
          FlattenGroupByAggregation(Ident("e"))(q.ast.body)
        }
      }
      "right" in {
        val q = quote {
          (e: Query[TestEntity]) =>
            qr1.leftJoin(e).on((a, b) => true).size
        }
        val e = intercept[IllegalStateException] {
          FlattenGroupByAggregation(Ident("e"))(q.ast.body)
        }
      }
    }
  }
}
