package io.getquill.sources.async.mysql

import io.getquill.sources.sql._
import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConverters._
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

class QueryResultTypeMysqlAsyncSpec extends QueryResultTypeSpec {
  val db = testMysqlDB

  def await[T](r: Future[T]) = Await.result(r, Duration.Inf)

  val insertedProducts = new ConcurrentLinkedQueue[Product]

  override def beforeAll = {
    await(db.run(deleteAll))
    val ids = await(db.run(productInsert)(productEntries))
    val inserted = (ids zip productEntries).map {
      case (id, prod) => prod.copy(id = id)
    }
    insertedProducts.addAll(inserted.asJava)
    ()
  }

  def products = insertedProducts.asScala.toList

  "return list" - {
    "select" in {
      await(db.run(selectAll)) must contain theSameElementsAs (products)
    }
    "map" in {
      await(db.run(map)) must contain theSameElementsAs (products.map(_.id))
    }
    "filter" in {
      await(db.run(filter)) must contain theSameElementsAs (products)
    }
    "withFilter" in {
      await(db.run(withFilter)) must contain theSameElementsAs (products)
    }
    "sortBy" in {
      await(db.run(sortBy)) must contain theSameElementsInOrderAs (products)
    }
    "take" in {
      await(db.run(take)) must contain theSameElementsAs (products)
    }
    "drop" in {
      await(db.run(drop)) must contain theSameElementsAs (products.drop(1))
    }
    "++" in {
      await(db.run(`++`)) must contain theSameElementsAs (products ++ products)
    }
    "unionAll" in {
      await(db.run(unionAll)) must contain theSameElementsAs (products ++ products)
    }
    "union" in {
      await(db.run(union)) must contain theSameElementsAs (products)
    }
    "join" in {
      await(db.run(join)) must contain theSameElementsAs (products zip products)
    }
    "distinct" in {
      await(db.run(distinct)) must contain theSameElementsAs (products.map(_.id).distinct)
    }
  }

  "return single result" - {
    "min" - {
      "some" in {
        await(db.run(minExists)) mustEqual Some(products.map(_.sku).min)
      }
      "none" in {
        await(db.run(minNonExists)) mustBe None
      }
    }
    "max" - {
      "some" in {
        await(db.run(maxExists)) mustBe Some(products.map(_.sku).max)
      }
      "none" in {
        await(db.run(maxNonExists)) mustBe None
      }
    }
    "avg" - {
      "some" in {
        await(db.run(avgExists)) mustBe Some(BigDecimal(products.map(_.sku).sum) / products.size)
      }
      "none" in {
        await(db.run(avgNonExists)) mustBe None
      }
    }
    "size" in {
      await(db.run(productSize)) mustEqual products.size
    }
    "paramlized size" in {
      await(db.run(paramlizedSize)(10000)) mustEqual 0
    }
    "nonEmpty" in {
      await(db.run(nonEmpty)) mustEqual true
    }
    "isEmpty" in {
      await(db.run(isEmpty)) mustEqual false
    }
  }
}
