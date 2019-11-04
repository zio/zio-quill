package io.getquill.norm

import io.getquill.Spec
import io.getquill.ast.Implicits._
import io.getquill.ast.{ Ast, Constant, Ident }

class TriviallyCheckableSpec extends Spec {

  def unapply(ast: Ast) = TriviallyCheckable.unapply(ast)
  val other = Ident("something")

  val True = Constant(true)
  val False = Constant(false)

  "constants must trivially reduce correctly" - {
    "const == const => true" in { unapply(Constant(123) +==+ Constant(123)) mustEqual Some(True) }
    "const == const => false" in { unapply(Constant(123) +==+ Constant(456)) mustEqual Some(False) }
    "const != const => true" in { unapply(Constant(123) +!=+ Constant(123)) mustEqual Some(False) }
    "const != const => false" in { unapply(Constant(123) +!=+ Constant(456)) mustEqual Some(True) }

    "const == other => none" in { unapply(Constant(123) +==+ other) mustEqual None }
    "const != other => none" in { unapply(Constant(123) +!=+ other) mustEqual None }
    "other == const => none" in { unapply(other +==+ Constant(123)) mustEqual None }
    "other != const => none" in { unapply(other +!=+ Constant(123)) mustEqual None }
    ()
  }

  "expressions must trivially reduce correctly" - {
    "true  || true  =>   true" in { unapply(True +||+ True) mustEqual Some(True) }
    "true  || false =>  false" in { unapply(True +||+ False) mustEqual Some(True) }
    "const || const =>  false" in { unapply(False +||+ True) mustEqual Some(True) }
    "true  && true  =>   true" in { unapply(True +&&+ True) mustEqual Some(True) }
    "true  && false =>  false" in { unapply(True +&&+ False) mustEqual Some(False) }
    "false && true  =>  false" in { unapply(False +&&+ True) mustEqual Some(False) }

    "false && other => false" in { unapply(False +&&+ other) mustEqual Some(False) }
    "other && false => false" in { unapply(other +&&+ False) mustEqual Some(False) }
    "true  && const =>  none" in { unapply(True +&&+ other) mustEqual None }
    "other && true  =>  none" in { unapply(other +&&+ True) mustEqual None }
    "other && other =>  none" in { unapply(other +&&+ other) mustEqual None }

    "true  || other => true" in { unapply(True +||+ other) mustEqual Some(True) }
    "other || true =>  true" in { unapply(other +||+ True) mustEqual Some(True) }
    "false || other => none" in { unapply(False +||+ other) mustEqual None }
    "other || false => none" in { unapply(other +||+ False) mustEqual None }
    "other || other => none" in { unapply(other +||+ other) mustEqual None }
    ()
  }

  "compound expressions must trivially reduce correctly" - {
    val compoundTrue = Constant(123) +==+ Constant(123)
    val compoundFalse = Constant(123) +==+ Constant(456)
    val compoundUnknown = Constant(123) +==+ other

    "true && true => true" in { unapply(compoundTrue +&&+ compoundTrue) mustEqual Some(True) }
    "true || true => true" in { unapply(compoundTrue +||+ compoundTrue) mustEqual Some(True) }
    "true && false => false" in { unapply(compoundTrue +&&+ compoundFalse) mustEqual Some(False) }
    "true || false => true" in { unapply(compoundTrue +||+ compoundFalse) mustEqual Some(True) }

    "true && none => none" in { unapply(compoundTrue +&&+ compoundUnknown) mustEqual None }
    "true || none => true" in { unapply(compoundTrue +||+ compoundUnknown) mustEqual Some(True) }
    "false && none => false" in { unapply(compoundFalse +&&+ compoundUnknown) mustEqual Some(False) }
    "false || none => true" in { unapply(compoundFalse +||+ compoundUnknown) mustEqual None }
    ()
  }
}
