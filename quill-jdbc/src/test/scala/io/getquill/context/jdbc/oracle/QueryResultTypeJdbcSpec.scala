package io.getquill.context.jdbc.oracle

import java.util.concurrent.ConcurrentLinkedQueue

import io.getquill.context.sql._

import scala.collection.JavaConverters._

class QueryResultTypeJdbcSpec extends QueryResultTypeSpec {

  override val context = testContext
  import context._

  def await[T](r: T) = r

  val insertedProducts = new ConcurrentLinkedQueue[Product]

  override def beforeAll = {
    context.run(deleteAll)
    val ids = context.run(liftQuery(productEntries).foreach(p => productInsert(p)))
    val inserted = (ids zip productEntries).map {
      case (id, prod) => prod.copy(id = id)
    }
    insertedProducts.addAll(inserted.asJava)
    ()
  }

  def products = insertedProducts.asScala.toList

  "return list" - {
    "select" in {
      await(context.run(selectAll)) must contain theSameElementsAs (products)
    }
    "map" in {
      await(context.run(map)) must contain theSameElementsAs (products.map(_.id))
    }
    "sortBy" in {
      await(context.run(sortBy)) must contain theSameElementsInOrderAs (products)
    }
    "take" in {
      await(context.run(take)) must contain theSameElementsAs (products)
    }
    "drop" in {
      await(context.run(drop)) must contain theSameElementsAs (products.drop(1))
    }
    "++" in {
      await(context.run(`++`)) must contain theSameElementsAs (products ++ products)
    }
    "unionAll" in {
      await(context.run(unionAll)) must contain theSameElementsAs (products ++ products)
    }
    "union" in {
      await(context.run(union)) must contain theSameElementsAs (products)
    }
    "join" in {
      await(context.run(join)) must contain theSameElementsAs (products zip products)
    }
    "distinct" in {
      await(context.run(distinct)) must contain theSameElementsAs (products.map(_.id).distinct)
    }
  }

  "return single result" - {
    "min" - {
      "some" in {
        await(context.run(minExists)) mustEqual Some(products.map(_.sku).min)
      }
      "none" in {
        await(context.run(minNonExists)) mustBe None
      }
    }
    "max" - {
      "some" in {
        await(context.run(maxExists)) mustBe Some(products.map(_.sku).max)
      }
      "none" in {
        await(context.run(maxNonExists)) mustBe None
      }
    }
    "avg" - {
      "some" in {
        await(context.run(avgExists)) mustBe Some(BigDecimal(products.map(_.sku).sum) / products.size)
      }
      "none" in {
        await(context.run(avgNonExists)) mustBe None
      }
    }
    "size" in {
      await(context.run(productSize)) mustEqual products.size
    }
    "parametrized size" in {
      await(context.run(parametrizedSize(lift(10000)))) mustEqual 0
    }
  }
}
