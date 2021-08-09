package io.getquill.ast

import io.getquill.Spec
import io.getquill.ast.Implicits._
import io.getquill.VIdent
import io.getquill.quat.Quat

class AstOpsSpec extends Spec {

  "+||+" - {
    "unapply" in {
      BinaryOperation(VIdent("a"), BooleanOperator.`||`, Constant.auto(true)) must matchPattern {
        case VIdent(a) +||+ Constant(t, _) if (a == "a" && t == true) =>
      }
    }
    "apply" in {
      (VIdent("a") +||+ Constant.auto(true)) must matchPattern {
        case BinaryOperation(VIdent(a), BooleanOperator.`||`, Constant(t, _)) if (a == "a" && t == true) =>
      }
    }
  }

  "+&&+" - {
    "unapply" in {
      BinaryOperation(VIdent("a"), BooleanOperator.`&&`, Constant.auto(true)) must matchPattern {
        case VIdent(a) +&&+ Constant(t, _) if (a == "a" && t == true) =>
      }
    }
    "apply" in {
      (VIdent("a") +&&+ Constant.auto(true)) must matchPattern {
        case BinaryOperation(VIdent(a), BooleanOperator.`&&`, Constant(t, _)) if (a == "a" && t == true) =>
      }
    }
  }

  "+==+" - {
    "unapply" in {
      BinaryOperation(VIdent("a"), EqualityOperator.`==`, Constant.auto(true)) must matchPattern {
        case VIdent(a) +==+ Constant(t, _) if (a == "a" && t == true) =>
      }
    }
    "apply" in {
      (VIdent("a") +==+ Constant.auto(true)) must matchPattern {
        case BinaryOperation(VIdent(a), EqualityOperator.`==`, Constant(t, _)) if (a == "a" && t == true) =>
      }
    }
  }

  "exist" - {
    "apply" in {
      IsNotNullCheck(VIdent("a")) must matchPattern {
        case BinaryOperation(VIdent(a), EqualityOperator.!=, NullValue) if (a == "a") =>
      }
    }
    "unapply" in {
      BinaryOperation(VIdent("a"), EqualityOperator.!=, NullValue) must matchPattern {
        case IsNotNullCheck(VIdent(a)) if (a == "a") =>
      }
    }
  }

  "empty" - {
    "apply" in {
      IsNullCheck(VIdent("a")) must matchPattern {
        case BinaryOperation(VIdent(a), EqualityOperator.==, NullValue) if (a == "a") =>
      }
    }
    "unapply" in {
      BinaryOperation(VIdent("a"), EqualityOperator.==, NullValue) must matchPattern {
        case IsNullCheck(VIdent(a)) if (a == "a") =>
      }
    }
  }

  "if exist" - {
    "apply" in {
      IfExist(VIdent("a"), VIdent("b"), VIdent("c")) must matchPattern {
        case If(BinaryOperation(VIdent(a), EqualityOperator.!=, NullValue), VIdent(b), VIdent(c)) if (a == "a" && b == "b" && c == "c") =>
      }
    }
    "unapply" in {
      If(BinaryOperation(VIdent("a"), EqualityOperator.!=, NullValue), VIdent("b"), VIdent("c")) must matchPattern {
        case IfExist(VIdent(a), VIdent(b), VIdent(c)) if (a == "a" && b == "b" && c == "c") =>
      }
    }
  }

  "if exist or null" - {
    "apply" in {
      IfExistElseNull(VIdent("a"), VIdent("b")) must matchPattern {
        case If(BinaryOperation(VIdent(a), EqualityOperator.!=, NullValue), VIdent(b), NullValue) if (a == "a" && b == "b") =>
      }
    }
    "unapply" in {
      If(BinaryOperation(VIdent("a"), EqualityOperator.!=, NullValue), VIdent("b"), NullValue) must matchPattern {
        case IfExistElseNull(VIdent(a), VIdent(b)) if (a == "a" && b == "b") =>
      }
    }
  }

  "returning matcher" - {
    val insert = Insert(Entity("Ent", List(), Quat.LeafProduct("prop")), List(Assignment(VIdent("p"), Property(VIdent("p"), "prop"), Constant.auto(123))))
    val r = VIdent("r")
    val prop = Property(r, "value")

    "must match returning" in {
      Returning(insert, r, prop) must matchPattern {
        case ReturningAction(`insert`, `r`, `prop`) =>
      }
    }

    "must match returning generated" in {
      ReturningGenerated(insert, r, prop) must matchPattern {
        case ReturningAction(`insert`, `r`, `prop`) =>
      }
    }

    "must not match anything else" in {
      insert mustNot matchPattern {
        case ReturningAction(_, _, _) =>
      }
    }
  }
}
