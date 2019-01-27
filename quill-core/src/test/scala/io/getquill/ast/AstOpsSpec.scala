package io.getquill.ast

import io.getquill.Spec
import io.getquill.ast.{ BinaryOperation => B }
import io.getquill.ast.{ EqualityOperator => EQ }
import io.getquill.ast.{ BooleanOperator => BO }
import io.getquill.ast.Implicits._

class AstOpsSpec extends Spec {

  "+||+" - {
    "unapply" in {
      B(Ident("a"), BO.`||`, Constant(true)) must matchPattern {
        case Ident(a) +||+ Constant(t) if (a == "a" && t == true) =>
      }
    }
    "apply" in {
      (Ident("a") +||+ Constant(true)) must matchPattern {
        case B(Ident(a), BO.`||`, Constant(t)) if (a == "a" && t == true) =>
      }
    }
  }

  "+&&+" - {
    "unapply" in {
      B(Ident("a"), BO.`&&`, Constant(true)) must matchPattern {
        case Ident(a) +&&+ Constant(t) if (a == "a" && t == true) =>
      }
    }
    "apply" in {
      (Ident("a") +&&+ Constant(true)) must matchPattern {
        case B(Ident(a), BO.`&&`, Constant(t)) if (a == "a" && t == true) =>
      }
    }
  }

  "+==+" - {
    "unapply" in {
      B(Ident("a"), EQ.`==`, Constant(true)) must matchPattern {
        case Ident(a) +==+ Constant(t) if (a == "a" && t == true) =>
      }
    }
    "apply" in {
      (Ident("a") +==+ Constant(true)) must matchPattern {
        case B(Ident(a), EQ.`==`, Constant(t)) if (a == "a" && t == true) =>
      }
    }
  }

  "exist" - {
    "apply" in {
      Exist(Ident("a")) must matchPattern {
        case B(Ident(a), EQ.!=, NullValue) if (a == "a") =>
      }
    }
    "unapply" in {
      B(Ident("a"), EQ.!=, NullValue) must matchPattern {
        case Exist(Ident(a)) if (a == "a") =>
      }
    }
  }

  "empty" - {
    "apply" in {
      Empty(Ident("a")) must matchPattern {
        case B(Ident(a), EQ.==, NullValue) if (a == "a") =>
      }
    }
    "unapply" in {
      B(Ident("a"), EQ.==, NullValue) must matchPattern {
        case Empty(Ident(a)) if (a == "a") =>
      }
    }
  }

  "if exist" - {
    "apply" in {
      IfExist(Ident("a"), Ident("b"), Ident("c")) must matchPattern {
        case If(B(Ident(a), EQ.!=, NullValue), Ident(b), Ident(c)) if (a == "a" && b == "b" && c == "c") =>
      }
    }
    "unapply" in {
      If(B(Ident("a"), EQ.!=, NullValue), Ident("b"), Ident("c")) must matchPattern {
        case IfExist(Ident(a), Ident(b), Ident(c)) if (a == "a" && b == "b" && c == "c") =>
      }
    }
  }

  "if exist or null" - {
    "apply" in {
      IfExistElseNull(Ident("a"), Ident("b")) must matchPattern {
        case If(B(Ident(a), EQ.!=, NullValue), Ident(b), NullValue) if (a == "a" && b == "b") =>
      }
    }
    "unapply" in {
      If(B(Ident("a"), EQ.!=, NullValue), Ident("b"), NullValue) must matchPattern {
        case IfExistElseNull(Ident(a), Ident(b)) if (a == "a" && b == "b") =>
      }
    }
  }
}
