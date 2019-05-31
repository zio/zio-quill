package io.getquill.norm

import io.getquill.Spec
import io.getquill.ast._
import io.getquill.ast.Implicits._
import io.getquill.testContext.{ quote, unquote }

class SimplifyNullChecksSpec extends Spec {

  val ia = Ident("a")
  val ib = Ident("b")
  val it = Ident("t")
  val ca = Constant("a")

  "center rule must" - {
    "apply when conditionals same" in {
      SimplifyNullChecks(
        IfExist(
          IfExistElseNull(ia, it),
          IfExistElseNull(ia, it),
          Ident("o")
        )
      ) mustEqual If(
          IsNotNullCheck(Ident("a")) +&&+ IsNotNullCheck(Ident("t")), Ident("t"), Ident("o")
        )
    }

    "apply left rule" in {
      SimplifyNullChecks(
        IfExist(IfExistElseNull(ia, ib), ca, it)
      ) mustEqual If(IsNotNullCheck(ia) +&&+ IsNotNullCheck(ib), ca, it)
    }

    "apply right rule" in {
      SimplifyNullChecks(
        IfExistElseNull(ia, IfExistElseNull(ib, it))
      ) mustEqual If(IsNotNullCheck(ia) +&&+ IsNotNullCheck(ib), it, NullValue)
    }

    "reduces when Option == Option(constant)" in {
      val q = quote {
        (a: Option[Int]) => a == Option(1)
      }
      SimplifyNullChecks(quote(unquote(q)).ast.body) mustEqual (OptionIsDefined(ia) +&&+ (ia +==+ OptionApply(Constant(1))))
    }

    "reduces when Option(constant) == Option" in {
      val q = quote {
        (a: Option[Int]) => Option(1) == a
      }
      SimplifyNullChecks(quote(unquote(q)).ast.body) mustEqual (OptionIsDefined(ia) +&&+ (OptionApply(Constant(1)) +==+ ia))
    }

    "reduces when Option != Option(constant)" in {
      val q = quote {
        (a: Option[Int]) => a != Option(1)
      }
      SimplifyNullChecks(quote(unquote(q)).ast.body) mustEqual (OptionIsEmpty(ia) +||+ (ia +!=+ OptionApply(Constant(1))))
    }

    "reduces when Option(constant) != Option" in {
      val q = quote {
        (a: Option[Int]) => Option(1) != a
      }
      SimplifyNullChecks(quote(unquote(q)).ast.body) mustEqual (OptionIsEmpty(ia) +||+ (OptionApply(Constant(1)) +!=+ ia))
    }
  }
}
