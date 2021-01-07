package io.getquill.context.sql.idiom

import io.getquill.Spec

class SqlIdiomOptionCompareSpec extends Spec {

  case class TwoIntsClassScope(one: Int, two: Int)

  // remove the === matcher from scalatest so that we can test === in Context.extra
  override def convertToEqualizer[T](left: T): Equalizer[T] = new Equalizer(left)

  "strictly checks non-ansi option operation" - {
    import io.getquill.context.sql.nonAnsiTestContext._
    import io.getquill.context.sql.{ nonAnsiTestContext => testContext }

    "Option == Option(constant)" in {
      val q = quote {
        qr1.filter(t => t.o == Option(1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND t.o = 1"
    }
    "Option(constant) == Option" in {
      val q = quote {
        qr1.filter(t => Option(1) == t.o)
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND 1 = t.o"
    }
    "Option != Option(constant)" in {
      val q = quote {
        qr1.filter(t => t.o != Option(1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NULL OR t.o <> 1"
    }
    "Option(constant) != Option" in {
      val q = quote {
        qr1.filter(t => Option(1) != t.o)
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NULL OR 1 <> t.o"
    }
    "Database-level === and =!= operators" - {
      import testContext.extras._

      "Option === Option" in {
        val q = quote {
          qr1.filter(t => t.o === t.o)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND t.o IS NOT NULL AND t.o = t.o"
      }
      "Option === Option(constant)" in {
        val q = quote {
          qr1.filter(t => t.o === Option(1))
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND t.o = 1"
      }
      "Option(constant) === Option" in {
        val q = quote {
          qr1.filter(t => Option(1) === t.o)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND 1 = t.o"
      }

      "Option =!= Option" in {
        val q = quote {
          qr1.filter(t => t.o =!= t.o)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND t.o IS NOT NULL AND t.o <> t.o"
      }
      "Option =!= Option(constant)" in {
        val q = quote {
          qr1.filter(t => t.o =!= Option(1))
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND t.o <> 1"
      }
      "Option(constant) =!= Option" in {
        val q = quote {
          qr1.filter(t => Option(1) =!= t.o)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND 1 <> t.o"
      }
    }
    "contains" in {
      val q = quote {
        qr1.filter(t => t.o.contains(1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o = 1"
    }
    "exists" in {
      val q = quote {
        qr1.filter(t => t.o.exists(op => op != 1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o <> 1"
    }
    "exists with null-check" in {
      val q = quote {
        qr1.filter(t => t.o.exists(op => if (op != 1) false else true))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND CASE WHEN t.o <> 1 THEN false ELSE true END"
    }
    "forall" in {
      val q = quote {
        qr1.filter(t => t.i != 1 && t.o.forall(op => op == 1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.i <> 1 AND (t.o IS NULL OR t.o = 1)"
    }
    "forall with null-check" in {
      val q = quote {
        qr1.filter(t => t.i != 1 && t.o.forall(op => if (op != 1) false else true))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.i <> 1 AND (t.o IS NULL OR t.o IS NOT NULL AND CASE WHEN t.o <> 1 THEN false ELSE true END)"
    }
    "embedded" - {
      case class TestEntity(optionalEmbedded: Option[EmbeddedEntity])
      case class EmbeddedEntity(value: Int) extends Embedded

      "exists" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.value == 1))
        }

        testContext.run(q).string mustEqual
          "SELECT t.value FROM TestEntity t WHERE t.value = 1"
      }
      "forall" in {
        "quote(query[TestEntity].filter(t => t.optionalEmbedded.forall(_.value == 1)))" mustNot compile
      }
    }
    "nested" - {
      case class TestEntity(optionalEmbedded: Option[EmbeddedEntity])
      case class EmbeddedEntity(optionalValue: Option[Int]) extends Embedded

      "contains" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.contains(1)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue = 1"
      }
      "exists" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.exists(_ == 1)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue = 1"
      }
      "exists with null-check" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.exists(v => if (v == 1) true else false)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue IS NOT NULL AND CASE WHEN t.optionalValue = 1 THEN true ELSE false END"
      }
      "forall" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.forall(_ == 1)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue IS NULL OR t.optionalValue = 1"
      }
      "forall with null-check" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.forall(v => if (v == 1) true else false)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue IS NULL OR t.optionalValue IS NOT NULL AND CASE WHEN t.optionalValue = 1 THEN true ELSE false END"
      }
    }
  }

  "optimizes checks for ansi option operation" - {
    import io.getquill.context.sql.testContext
    import io.getquill.context.sql.testContext._

    "Option == Option(constant)" in {
      val q = quote {
        qr1.filter(t => t.o == Option(1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o = 1"
    }
    "Option(constant) == Option" in {
      val q = quote {
        qr1.filter(t => Option(1) == t.o)
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE 1 = t.o"
    }
    "Option != Option(constant)" in {
      val q = quote {
        qr1.filter(t => t.o != Option(1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NULL OR t.o <> 1"
    }
    "Option(constant) != Option" in {
      val q = quote {
        qr1.filter(t => Option(1) != t.o)
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NULL OR 1 <> t.o"
    }
    "Database-level === and =!= operators" - {
      import testContext.extras._

      "Option === Option" in {
        val q = quote {
          qr1.filter(t => t.o === t.o)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o = t.o"
      }
      "Option === Option(constant)" in {
        val q = quote {
          qr1.filter(t => t.o === Option(1))
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o = 1"
      }
      "Option(constant) === Option" in {
        val q = quote {
          qr1.filter(t => Option(1) === t.o)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE 1 = t.o"
      }

      "Option =!= Option" in {
        val q = quote {
          qr1.filter(t => t.o =!= t.o)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o <> t.o"
      }
      "Option =!= Option(constant)" in {
        val q = quote {
          qr1.filter(t => t.o =!= Option(1))
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o <> 1"
      }
      "Option(constant) =!= Option" in {
        val q = quote {
          qr1.filter(t => Option(1) =!= t.o)
        }
        testContext.run(q).string mustEqual
          "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE 1 <> t.o"
      }
    }
    "contains" in {
      val q = quote {
        qr1.filter(t => t.o.contains(1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o = 1"
    }
    "exists" in {
      val q = quote {
        qr1.filter(t => t.o.exists(op => op != 1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o <> 1"
    }
    "exists with null-check" in {
      val q = quote {
        qr1.filter(t => t.o.exists(op => if (op != 1) false else true))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.o IS NOT NULL AND CASE WHEN t.o <> 1 THEN false ELSE true END"
    }
    "forall" in {
      val q = quote {
        qr1.filter(t => t.i != 1 && t.o.forall(op => op == 1))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.i <> 1 AND (t.o IS NULL OR t.o = 1)"
    }
    "forall with null-check" in {
      val q = quote {
        qr1.filter(t => t.i != 1 && t.o.forall(op => if (op != 1) false else true))
      }
      testContext.run(q).string mustEqual
        "SELECT t.s, t.i, t.l, t.o, t.b FROM TestEntity t WHERE t.i <> 1 AND (t.o IS NULL OR t.o IS NOT NULL AND CASE WHEN t.o <> 1 THEN false ELSE true END)"
    }
    "embedded" - {
      case class TestEntity(optionalEmbedded: Option[EmbeddedEntity])
      case class EmbeddedEntity(value: Int) extends Embedded

      "exists" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.value == 1))
        }

        testContext.run(q).string mustEqual
          "SELECT t.value FROM TestEntity t WHERE t.value = 1"
      }
      "forall" in {
        "quote(query[TestEntity].filter(t => t.optionalEmbedded.forall(_.value == 1)))" mustNot compile
      }
    }
    "nested" - {
      case class TestEntity(optionalEmbedded: Option[EmbeddedEntity])
      case class EmbeddedEntity(optionalValue: Option[Int]) extends Embedded

      "contains" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.contains(1)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue = 1"
      }
      "exists" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.exists(_ == 1)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue = 1"
      }
      "exists with null-check" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.exists(v => if (v == 1) true else false)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue IS NOT NULL AND CASE WHEN t.optionalValue = 1 THEN true ELSE false END"
      }
      "forall" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.forall(_ == 1)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue IS NULL OR t.optionalValue = 1"
      }
      "forall with null-check" in {
        val q = quote {
          query[TestEntity].filter(t => t.optionalEmbedded.exists(_.optionalValue.forall(v => if (v == 1) true else false)))
        }

        testContext.run(q).string mustEqual
          "SELECT t.optionalValue FROM TestEntity t WHERE t.optionalValue IS NULL OR t.optionalValue IS NOT NULL AND CASE WHEN t.optionalValue = 1 THEN true ELSE false END"
      }
    }
  }
}
