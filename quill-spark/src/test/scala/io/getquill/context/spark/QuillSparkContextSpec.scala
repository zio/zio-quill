package io.getquill.context.spark

import io.getquill.Spec
import scala.util.Success

class QuillSparkContextSpec extends Spec {

  import sqlContext.implicits._
  import testContext._

  val entities = Seq(Test(1, 2))

  "toQuery" - {
    "seq" in {
      testContext.run(entities.toQuery).collect.toList mustEqual
        entities
    }

    "dataset" in {
      val q = sqlContext.createDataset(entities).toQuery
      testContext.run(q).collect.toList mustEqual
        entities
    }

    "rdd" in {
      val q = sqlContext.sparkContext.parallelize(entities).toQuery
      testContext.run(q).collect.toList mustEqual
        entities
    }
  }

  "close is a no-op" in {
    testContext.close()
  }

  "probe is a no-op" in {
    testContext.probe("stmt") mustEqual Success(Unit)
  }

  "decoders aren't used and throw an exception" - {
    "dummy decoder" in {
      val d = dummyDecoder[Int]
      intercept[IllegalStateException] {
        d(0, {})
      }
    }
    "mapped decoder" in {
      implicit val m = MappedEncoding[String, Int](_.toInt)
      val d = mappedDecoder[String, Int]
      intercept[IllegalStateException] {
        d(0, {})
      }
    }
  }

  "query single" in {
    val q = quote {
      entities.toQuery.map(t => t.i).max
    }
    testContext.run(q).collect.toList mustEqual
      List(Some(1))
  }
}