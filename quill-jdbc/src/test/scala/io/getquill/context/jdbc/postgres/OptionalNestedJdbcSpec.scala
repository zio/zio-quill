package io.getquill.context.jdbc.postgres

import io.getquill.context.encoding.OptionalNestedSpec

class OptionalProductEncodingJdbcSpec extends OptionalNestedSpec {

  val context = testContext
  import testContext._

  override protected def beforeEach() = {
    import Setup._
    testContext.run(query[Contact].delete)
    ()
  }

  "1.Optional Inner Product" - {
    import `1.Optional Inner Product`._
    "1.Ex1 - Not null inner product" in {
      context.run(`1.Ex1 - Not null inner product insert`)
      context.run(data) mustEqual List(`1.Ex1 - Not null inner product result`)
    }
    "1.Ex1 Auto - Not null inner product" in {
      val result = `1.Ex1 - Not null inner product result`
      context.run(data.insertValue(lift(result)))
      context.run(data) mustEqual List(result)
    }

    "1.Ex2 - null inner product" in {
      context.run(`1.Ex2 - null inner product insert`)
      context.run(data) mustEqual List(`1.Ex2 - null inner product result`)
    }
    "1.Ex2 Auto - null inner product" in {
      val result = `1.Ex2 - null inner product result`
      context.run(data.insertValue(lift(result)))
      context.run(data) mustEqual List(result)
    }
  }

  "2.Optional Inner Product" - {
    import `2.Optional Inner Product with Optional Leaf`._
    "2.Ex1 - Not null inner product" in {
      context.run(`2.Ex1 - not-null insert`)
      context.run(data) mustEqual List(`2.Ex1 - not-null result`)
    }
    "2.Ex1 Auto - Not null inner product" in {
      val result = `2.Ex1 - not-null result`
      context.run(data.insertValue(lift(result)))
      context.run(data) mustEqual List(result)
    }

    "2.Ex2 - Not null inner product" in {
      context.run(`2.Ex2 - Null inner product insert`)
      context.run(data) mustEqual List(`2.Ex2 - Null inner product result`)
    }
    "2.Ex2 Auto - Not null inner product" in {
      val result = `2.Ex2 - Null inner product result`
      context.run(data.insertValue(lift(result)))
      context.run(data) mustEqual List(result)
    }

    "2.Ex3 - Null inner leaf" in {
      context.run(`2.Ex3 - Null inner leaf insert`)
      context.run(data) mustEqual List(`2.Ex3 - Null inner leaf result`)
    }
    "2.Ex3 Auto - Null inner leaf" in {
      val result = `2.Ex3 - Null inner leaf result`
      context.run(data.insertValue(lift(result)))
      context.run(data) mustEqual List(result)
    }
  }

  "3.Optional Nested Inner Product" - {
    import `3.Optional Nested Inner Product`._
    "3.Ex1 - Null inner product insert" in {
      context.run(`3.Ex1 - Null inner product insert`)
      context.run(data) mustEqual List(`3.Ex1 - Null inner product result`)
    }
    "3.Ex1 Auto - Null inner product insert" in {
      val result = `3.Ex1 - Null inner product result`
      context.run(data.insertValue(lift(result)))
      context.run(data) mustEqual List(result)
    }

    "3.Ex2 - Null inner leaf" in {
      context.run(`3.Ex2 - Null inner leaf insert`)
      context.run(data) mustEqual List(`3.Ex2 - Null inner leaf result`)
    }
    "3.Ex2 Auto - Null inner leaf" in {
      val result = `3.Ex2 - Null inner leaf result`
      context.run(data.insertValue(lift(result)))
      context.run(data) mustEqual List(result)
    }
  }
}
