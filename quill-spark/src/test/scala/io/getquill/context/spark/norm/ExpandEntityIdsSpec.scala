package io.getquill.context.spark.norm

import io.getquill.Spec
import io.getquill.context.spark.{ sqlContext, testContext }

case class Test(i: Int, j: Int, s: String)
case class TestHolder(ta: Test, tb: Test)
case class TestHolderOtherData(ta: Test, otherData: String)
case class TestHolderHolder(tha: TestHolder, thb: TestHolder)
case class TestHolderHolderOneSide(tha: TestHolder, tbb: Test)
case class TupleHolderTest(tup: (Int, Int), otherData: String)
case class NestedTupleHolderTest(tup: (Int, Test), otherData: String)
case class ParentNestedTupleHolderTest(tup: NestedTupleHolderTest, otherData: String)
case class SuperParentNestedTupleHolderTest(tup: (NestedTupleHolderTest, Test), otherData: String)
case class SingleElement(i: Int)
case class SingleElementSingleHolder(sma: SingleElement)
case class SingleElementMultiHolder(t: Test, sm: SingleElement)

class ExpandEntityIdsSpec extends Spec {

  import testContext._
  import sqlContext.implicits._

  val ent = Test(1, 2, "3")
  val entities = Seq(ent)

  val qr1 = liftQuery(entities.toDS)
  val qr2 = liftQuery(entities.toDS)
  val qr3 = liftQuery(entities.toDS)
  val qr4 = liftQuery(entities.toDS)

  val s0 = liftQuery(Seq(Tuple1(1), Tuple1(2)).toDS)
  val s1 = liftQuery(Seq(SingleElement(1), SingleElement(2)).toDS)
  val s2 = liftQuery(Seq(SingleElementSingleHolder(SingleElement(1)), SingleElementSingleHolder(SingleElement(2))).toDS)
  val s3 = liftQuery(Seq(SingleElementMultiHolder(Test(1, 2, "3"), SingleElement(1)), SingleElementMultiHolder(Test(1, 2, "3"), SingleElement(2))).toDS)

  "decomposition" - {
    "tuple decomposition" in {
      val j1 = testContext.run {
        qr1.join(qr2).on(_.i == _.i)
      }

      val q = quote {
        for {
          (ja, jb) <- liftQuery(j1)
        } yield (ja, jb)
      }
      testContext.run(q).collect.toList mustEqual List((Test(1, 2, "3"), Test(1, 2, "3")))
    }

    "tuple decomposition combination" in {
      val j1 = testContext.run {
        qr1.join(qr2).on(_.i == _.i)
      }

      val q = quote {
        for {
          (ja, jb) <- liftQuery(j1)
          c <- qr3 if (jb.i == c.i)
        } yield (ja, jb, c)
      }
      testContext.run(q).collect.toList mustEqual List((Test(1, 2, "3"), Test(1, 2, "3"), Test(1, 2, "3")))
    }

    "tuple decomposition combination via join" in {
      val j1 = testContext.run {
        qr1.join(qr2).on(_.i == _.i)
      }

      val q = quote {
        for {
          (ja, jb) <- liftQuery(j1)
          c <- qr3.join(_.i == jb.i)
        } yield (ja, jb, c)
      }
      testContext.run(q).collect.toList mustEqual List((Test(1, 2, "3"), Test(1, 2, "3"), Test(1, 2, "3")))
    }
  }

  "single element nesting cases" - {

    "single tuple" in {
      val q = quote {
        for {
          a <- qr1
          sm <- s0 if sm._1 == a.i
        } yield (a, sm)
      }
      testContext.run(q).collect.toList mustEqual List((Test(1, 2, "3"), Tuple1(1)))
    }

    "single tuple yield" in {
      val q = quote {
        for {
          a <- qr1
          sm <- s0 if sm._1 == a.i
        } yield (sm)
      }
      testContext.run(q).collect.toList mustEqual List(Tuple1(1))
    }

    "single element in nested in tuple" in {
      val q = quote {
        for {
          a <- qr1
          sm <- s1 if sm.i == a.i
        } yield (a, sm)
      }
      testContext.run(q).collect.toList mustEqual List((Test(1, 2, "3"), SingleElement(1)))
    }

    "two-level single element, nested in tuple" in {
      val q = quote {
        for {
          a <- qr1
          sm <- s2 if sm.sma.i == a.i
        } yield (a, sm)
      }
      testContext.run(q).collect.toList mustEqual List((Test(1, 2, "3"), SingleElementSingleHolder(SingleElement(1))))
    }

    "single element, nested in case class ad-hoc case class out" in {
      val q = quote {
        for {
          a <- qr1
          sm <- s1 if sm.i == a.i
        } yield SingleElementMultiHolder(a, sm)
      }
      testContext.run(q).collect.toList mustEqual List((SingleElementMultiHolder(Test(1, 2, "3"), SingleElement(1))))
    }

    "single element, nested in case class ad-hoc case class out further surrounded by tuple" in {
      val q = quote {
        for {
          a <- qr1
          sm <- s1 if sm.i == a.i
        } yield (sm, SingleElementMultiHolder(a, sm))
      }
      testContext.run(q).collect.toList mustEqual List((SingleElement(1), SingleElementMultiHolder(Test(1, 2, "3"), SingleElement(1))))
    }

    "single element, nested in case class with case class out further surrounded by tuple" in {
      val q = quote {
        for {
          a <- qr1
          sm <- s2 if sm.sma.i == a.i
        } yield (sm.sma, (a, sm))
      }
      testContext.run(q).collect.toList mustEqual List((SingleElement(1), (Test(1, 2, "3"), SingleElementSingleHolder(SingleElement(1)))))
    }

    "single element, nested in two-element case class with case class out further surrounded by tuple" in {
      val q = quote {
        for {
          a <- qr1
          sm <- s3 if sm.sm.i == a.i
        } yield sm
      }
      testContext.run(q).collect.toList mustEqual List(SingleElementMultiHolder(Test(1, 2, "3"), SingleElement(1)))
    }
  }

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
    "entities inside tuples with alternative element" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
        } yield (a, TestHolderOtherData(b, b.s))
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => (e, TestHolderOtherData(e, e.s)))
    }
    "entities inside tuples with alternative element - true entity" in {
      val ds = Seq(TestHolderOtherData(ent, ent.s)).toDS
      val q = quote {
        for {
          a <- liftQuery(ds)
        } yield a
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => TestHolderOtherData(e, e.s))
    }
    "entities inside tuples with alternative element - true entity with join" in {
      val ds = Seq(TestHolderOtherData(ent, ent.s)).toDS
      val q = quote {
        for {
          a <- liftQuery(ds)
          b <- qr2 if (a.ta.i + 1) == b.j
        } yield (a, b)
      }
      testContext.run(q).collect.toList mustEqual entities.map(e => (TestHolderOtherData(e, e.s), e))
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
    "entities inside ad-hoc case classes - dynamic" in {
      val q = quote {
        for {
          a <- qr1
          b <- qr2 if (a.i + 1) == b.j
        } yield TestHolder(a, b)
      }
      testContext.run(q.dynamic).collect.toList mustEqual entities.map(e => TestHolder(e, e))
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

    // TODO Ad-Hoc case class union inside for comprehension
    // TODO Tuple union inside for comprehension
  }
}
