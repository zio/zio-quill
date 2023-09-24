package io.getquill.context.qzio.jasync.postgres

import io.getquill.context.sql.base.QueryResultTypeSpec

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._
import scala.math.BigDecimal.int2bigDecimal

class QueryResultTypePostgresAsyncSpec extends QueryResultTypeSpec with ZioSpec {

  import context._

  val insertedProducts = new ConcurrentLinkedQueue[Product]

  override def beforeAll = {
    runSyncUnsafe(testContext.run(deleteAll))
    val ids = runSyncUnsafe(testContext.run(liftQuery(productEntries).foreach(e => productInsert(e))))
    val inserted = (ids zip productEntries).map { case (id, prod) =>
      prod.copy(id = id)
    }
    insertedProducts.addAll(inserted.asJava)
    ()
  }

  def products = insertedProducts.asScala.toList

  "return list" - {
    "select" in {
      runSyncUnsafe(testContext.run(selectAll)) must contain theSameElementsAs (products)
    }
    "map" in {
      runSyncUnsafe(testContext.run(map)) must contain theSameElementsAs (products.map(_.id))
    }
    "filter" in {
      runSyncUnsafe(testContext.run(filter)) must contain theSameElementsAs (products)
    }
    "withFilter" in {
      runSyncUnsafe(testContext.run(withFilter)) must contain theSameElementsAs (products)
    }
    "sortBy" in {
      runSyncUnsafe(testContext.run(sortBy)) must contain theSameElementsInOrderAs (products)
    }
    "take" in {
      runSyncUnsafe(testContext.run(take)) must contain theSameElementsAs (products)
    }
    "drop" in {
      runSyncUnsafe(testContext.run(drop)) must contain theSameElementsAs (products.drop(1))
    }
    "++" in {
      runSyncUnsafe(testContext.run(`++`)) must contain theSameElementsAs (products ++ products)
    }
    "unionAll" in {
      runSyncUnsafe(testContext.run(unionAll)) must contain theSameElementsAs (products ++ products)
    }
    "union" in {
      runSyncUnsafe(testContext.run(union)) must contain theSameElementsAs (products)
    }
    "join" in {
      runSyncUnsafe(testContext.run(join)) must contain theSameElementsAs (products zip products)
    }
    "distinct" in {
      runSyncUnsafe(testContext.run(distinct)) must contain theSameElementsAs (products.map(_.id).distinct)
    }
  }

  "return single result" - {
    "min" - {
      "some" in {
        runSyncUnsafe(testContext.run(minExists)) mustEqual Some(products.map(_.sku).min)
      }
      "none" in {
        runSyncUnsafe(testContext.run(minNonExists)) mustBe None
      }
    }
    "max" - {
      "some" in {
        runSyncUnsafe(testContext.run(maxExists)) mustBe Some(products.map(_.sku).max)
      }
      "none" in {
        runSyncUnsafe(testContext.run(maxNonExists)) mustBe None
      }
    }
    "avg" - {
      "some" in {
        runSyncUnsafe(testContext.run(avgExists)) mustBe Some(BigDecimal(products.map(_.sku).sum) / products.size)
      }
      "none" in {
        runSyncUnsafe(testContext.run(avgNonExists)) mustBe None
      }
    }
    "size" in {
      runSyncUnsafe(testContext.run(productSize)) mustEqual products.size
    }
    "parametrized size" in {
      runSyncUnsafe(testContext.run(parametrizedSize(lift(10000)))) mustEqual 0
    }
    "nonEmpty" in {
      runSyncUnsafe(testContext.run(nonEmpty)) mustEqual true
    }
    "isEmpty" in {
      runSyncUnsafe(testContext.run(isEmpty)) mustEqual false
    }
  }
}
