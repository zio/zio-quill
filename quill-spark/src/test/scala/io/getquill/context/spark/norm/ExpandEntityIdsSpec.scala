package io.getquill.context.spark.norm

import io.getquill.Spec
import io.getquill.context.spark.{ sqlContext, testContext }

case class Test(i: Int, j: Int, s: String)
case class TestHolder(ta: Test, tb: Test)
case class TestHolderHolder(tha: TestHolder, thb: TestHolder)
case class TestHolderHolderOneSide(tha: TestHolder, tbb: Test)
case class TupleHolderTest(tup: (Int, Int), otherData: String)
case class NestedTupleHolderTest(tup: (Int, Test), otherData: String)
case class ParentNestedTupleHolderTest(tup: NestedTupleHolderTest, otherData: String)
case class SuperParentNestedTupleHolderTest(tup: (NestedTupleHolderTest, Test), otherData: String)

class ExpandEntityIdsSpec extends Spec {

  import testContext._
  import sqlContext.implicits._

  val entities = Seq(Test(1, 2, "3"))

  val qr1 = liftQuery(entities.toDS)
  val qr2 = liftQuery(entities.toDS)
  val qr3 = liftQuery(entities.toDS)
  val qr4 = liftQuery(entities.toDS)

  "allows nested returns" - {
    "entities inside tuples" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
        } yield (a, b)
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => (e, e))
    }
    "entities inside multiple levels of tuples" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
          c <- qr3 if (a.i + 1) == c.j && (b.i + 1) == c.j
        } yield (a, (b, c))
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => (e, (e, e)))
    }
    "entities inside ad-hoc case classes" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
        } yield TestHolder(a, b)
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => TestHolder(e, e))
    }
    "regular entities and entities inside ad-hoc case classes" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
        } yield (a, b, TestHolder(a, b))
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => (e, e, TestHolder(e, e)))
    }
    "entities inside ad-hoc case classes multiple levels" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
          c <- qr3 if (a.i + 1) == c.j
          d <- qr4 if (c.i + 1) == d.j
        } yield TestHolderHolder(TestHolder(a, b), TestHolder(a, b))
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => TestHolderHolder(TestHolder(e, e), TestHolder(e, e)))
    }
    "entities inside ad-hoc case classes multiple levels different levels" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
          c <- qr3 if (a.i + 1) == c.j
          d <- qr4 if (c.i + 1) == d.j
        } yield TestHolderHolderOneSide(TestHolder(a, b), a)
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => TestHolderHolderOneSide(TestHolder(e, e), e))
    }
    "entities inside ad-hoc case classes multiple levels mixed with tuples" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
          c <- qr3 if (a.i + 1) == c.j
          d <- qr4 if (c.i + 1) == d.j
        } yield (TestHolderHolder(TestHolder(a, b), TestHolder(a, b)), TestHolder(a, b))
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => (TestHolderHolder(TestHolder(e, e), TestHolder(e, e)), TestHolder(e, e)))
    }
    "entities inside tuples - with distinct" in {
      val q = quote {
        (for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
        } yield (a, b)).distinct
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => (e, e))
    }
    "entities inside ad-hoc case classes - with distinct" in {
      val q = quote {
        (for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
        } yield TestHolder(a, b)).distinct
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => TestHolder(e, e))
    }
    "entities inside tuples with unions" in {
      val q = quote {
        qr1.map(r => (r, r)) ++ (qr2.filter(q => (q.i + 1) == q.j).map(r => (r, r)))
      }
      testContext.run(q).collect.toList mustEqual (entities.map(e => (e, e)) union entities.map(e => (e, e)))
    }
    "entities inside tuples with sub-unions" in {
      val q = quote {
        for {
          a <- (qr1 ++ qr2)
          b <- qr2 if (a.i + 1) == b.j
        } yield (a, b)
      }
      testContext.run(q).collect.toList mustEqual (entities.map(e => (e, e)) union entities.map(e => (e, e)))
    }
  }

  "advanced tuple nesting cases" - {
    "test tuple holder" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
          c <- qr3 if (a.i + 1) == c.j
          d <- qr4 if (c.i + 1) == d.j
        } yield TupleHolderTest((a.i, b.j), c.s)
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => TupleHolderTest((e.i, e.j), e.s))
    }
    "test nested tuple holder" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
          c <- qr3 if (a.i + 1) == c.j
          d <- qr4 if (c.i + 1) == d.j
        } yield NestedTupleHolderTest((a.i, b), c.s)
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => NestedTupleHolderTest((e.i, e), e.s))
    }
    "test nested tuple holder inside parent" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
          c <- qr3 if (a.i + 1) == c.j
          d <- qr4 if (c.i + 1) == d.j
        } yield ParentNestedTupleHolderTest(NestedTupleHolderTest((a.i, b), c.s), c.s)
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => ParentNestedTupleHolderTest(NestedTupleHolderTest((e.i, e), e.s), e.s))
    }
    "test nested tuple holder inside parent inside super tuple" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
          c <- qr3 if (a.i + 1) == c.j
          d <- qr4 if (c.i + 1) == d.j
        } yield SuperParentNestedTupleHolderTest((NestedTupleHolderTest((a.i, b), c.s), c), c.s)
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => SuperParentNestedTupleHolderTest((NestedTupleHolderTest((e.i, e), e.s), e), e.s))
    }
  }
}
