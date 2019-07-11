package io.getquill.context.finagle.postgres

import com.twitter.util.{ Await, Future, Throw }

import io.getquill.context.sql.ProductSpec

class TransactionSpec extends ProductSpec {
  val context = testContext
  import context._

  def await[T](future: Future[T]) = Await.result(future)

  val p = Product(0L, "Scala Compiler", 1L)

  "If outer transaction fails, inner transactions shouldn't commit" in {
    val id: Long = await {
      context.transaction {
        for {
          id <- context.transaction {
            context.run(productInsert(lift(p)))
          }
          Throw(_) <- context.transaction {
            context.run(quote {
              query[Product].insert(lift(p.copy(id = id)))
            }).liftToTry
          }
        } yield id
      }
    }
    // Since a query inside a transaction failed, the outermost transaction had
    // to rollback.
    val res: List[Product] = await { context.run(productById(lift(id))) }
    res mustEqual List()
  }

  "Transaction inside transaction should not open a new client" in {
    val res: Product = await {
      context.transaction {
        for {
          id: Long <- context.run(productInsert(lift(p)))
          // A subtransaction should have access to the previous queries of an
          // outer transaction.
          res: List[Product] <- context.transaction {
            context.run(productById(lift(id)))
          }
        } yield res.head
      }
    }
    res mustEqual p.copy(id = res.id)
  }

  override def beforeAll = {
    await(context.run(quote { query[Product].delete }))
    ()
  }
}
