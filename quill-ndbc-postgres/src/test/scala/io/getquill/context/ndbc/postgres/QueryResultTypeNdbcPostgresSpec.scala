package io.getquill.context.ndbc.postgres

import java.util.concurrent.ConcurrentLinkedQueue

import scala.BigDecimal
import scala.jdk.CollectionConverters._
import scala.math.BigDecimal.int2bigDecimal

import io.getquill.context.sql.QueryResultTypeSpec

class QueryResultTypeNdbcPostgresSpec extends QueryResultTypeSpec {

  val context = testContext
  import context._

  val insertedProducts = new ConcurrentLinkedQueue[Product]

  override def beforeAll = {
    get(context.run(deleteAll))
    val ids = get(context.run(liftQuery(productEntries).foreach(e => productInsert(e))))
    val inserted = (ids zip productEntries).map {
      case (id, prod) => prod.copy(id = id)
    }
    insertedProducts.addAll(inserted.asJava)
    ()
  }

  def products = insertedProducts.asScala.toList

  "return list" - {
    "select" in {
      get(context.run(selectAll)) must contain theSameElementsAs (products)
    }
    "map" in {
      get(context.run(map)) must contain theSameElementsAs (products.map(_.id))
    }
    "filter" in {
      get(context.run(filter)) must contain theSameElementsAs (products)
    }
    "withFilter" in {
      get(context.run(withFilter)) must contain theSameElementsAs (products)
    }
    "sortBy" in {
      get(context.run(sortBy)) must contain theSameElementsInOrderAs (products)
    }
    "take" in {
      get(context.run(take)) must contain theSameElementsAs (products)
    }
    "drop" in {
      get(context.run(drop)) must contain theSameElementsAs (products.drop(1))
    }
    "++" in {
      get(context.run(`++`)) must contain theSameElementsAs (products ++ products)
    }
    "unionAll" in {
      get(context.run(unionAll)) must contain theSameElementsAs (products ++ products)
    }
    "union" in {
      get(context.run(union)) must contain theSameElementsAs (products)
    }
    "join" in {
      get(context.run(join)) must contain theSameElementsAs (products zip products)
    }
    "distinct" in {
      get(context.run(distinct)) must contain theSameElementsAs (products.map(_.id).distinct)
    }
  }

  "return single result" - {
    "min" - {
      "some" in {
        get(context.run(minExists)) mustEqual Some(products.map(_.sku).min)
      }
      "none" in {
        get(context.run(minNonExists)) mustBe None
      }
    }
    "max" - {
      "some" in {
        get(context.run(maxExists)) mustBe Some(products.map(_.sku).max)
      }
      "none" in {
        get(context.run(maxNonExists)) mustBe None
      }
    }
    "avg" - {
      "some" in {
        get(context.run(avgExists)) mustBe Some(BigDecimal(products.map(_.sku).sum) / products.size)
      }
      "none" in {
        get(context.run(avgNonExists)) mustBe None
      }
    }
    "size" in {
      get(context.run(productSize)) mustEqual products.size
    }
    "parametrized size" in {
      get(context.run(parametrizedSize(lift(10000)))) mustEqual 0
    }
    "nonEmpty" in {
      get(context.run(nonEmpty)) mustEqual true
    }
    "isEmpty" in {
      get(context.run(isEmpty)) mustEqual false
    }
  }
}