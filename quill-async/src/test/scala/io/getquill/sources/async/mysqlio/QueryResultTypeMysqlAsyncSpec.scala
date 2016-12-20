package io.getquill.sources.async.mysqlio

import io.getquill.sources.sql._
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

class QueryResultTypeMysqlAsyncSpec extends QueryResultTypeSpec {
  val db = testMysqlIO

  def await[T](r: Future[T]) = Await.result(r, Duration.Inf)

  val insertedProducts = new ConcurrentLinkedQueue[Product]

  override def beforeAll = {
    await(db.run(deleteAll).unsafePerformIO)
    val ids = await(db.run(productInsert)(productEntries).unsafePerformIO)
    val inserted = (ids zip productEntries).map {
      case (id, prod) => prod.copy(id = id)
    }
    insertedProducts.addAll(inserted.asJava)
    ()
  }

  def products = insertedProducts.asScala.toList

  "return list" - {
    "select" in {
      await(db.run(selectAll).unsafePerformIO) must contain theSameElementsAs (products)
    }
    "map" in {
      await(db.run(map).unsafePerformIO) must contain theSameElementsAs (products.map(_.id))
    }
    "filter" in {
      await(db.run(filter).unsafePerformIO) must contain theSameElementsAs (products)
    }
    "withFilter" in {
      await(db.run(withFilter).unsafePerformIO) must contain theSameElementsAs (products)
    }
    "sortBy" in {
      await(db.run(sortBy).unsafePerformIO) must contain theSameElementsInOrderAs (products)
    }
    "take" in {
      await(db.run(take).unsafePerformIO) must contain theSameElementsAs (products)
    }
    "drop" in {
      await(db.run(drop).unsafePerformIO) must contain theSameElementsAs (products.drop(1))
    }
    "++" in {
      await(db.run(`++`).unsafePerformIO) must contain theSameElementsAs (products ++ products)
    }
    "unionAll" in {
      await(db.run(unionAll).unsafePerformIO) must contain theSameElementsAs (products ++ products)
    }
    "union" in {
      await(db.run(union).unsafePerformIO) must contain theSameElementsAs (products)
    }
    "join" in {
      await(db.run(join).unsafePerformIO) must contain theSameElementsAs (products zip products)
    }
    "distinct" in {
      await(db.run(distinct).unsafePerformIO) must contain theSameElementsAs (products.map(_.id).distinct)
    }
  }

  "return single result" - {
    "min" - {
      "some" in {
        await(db.run(minExists).unsafePerformIO) mustEqual Some(products.map(_.sku).min)
      }
      "none" in {
        await(db.run(minNonExists).unsafePerformIO) mustBe None
      }
    }
    "max" - {
      "some" in {
        await(db.run(maxExists).unsafePerformIO) mustBe Some(products.map(_.sku).max)
      }
      "none" in {
        await(db.run(maxNonExists).unsafePerformIO) mustBe None
      }
    }
    "avg" - {
      "some" in {
        await(db.run(avgExists).unsafePerformIO) mustBe Some(BigDecimal(products.map(_.sku).sum) / products.size)
      }
      "none" in {
        await(db.run(avgNonExists).unsafePerformIO) mustBe None
      }
    }
    "size" in {
      await(db.run(productSize).unsafePerformIO) mustEqual products.size
    }
    "parametrized size" in {
      await(db.run(parametrizedSize)(10000).unsafePerformIO) mustEqual 0
    }
    "nonEmpty" in {
      await(db.run(nonEmpty).unsafePerformIO) mustEqual true
    }
    "isEmpty" in {
      await(db.run(isEmpty).unsafePerformIO) mustEqual false
    }
  }
}
