package io.getquill.norm

import io.getquill.ast._
import io.getquill.quat._
import io.getquill.Spec
import io.getquill.testContext._
import scala.collection.immutable.{ Map => IMap }

class StablizeLiftsSpec extends Spec {

  case class Foo(id: Long)

  "stablize lifts" - {
    "ScalarValueLift" in {
      val scalarValue = 1
      val ast = quote(lift(scalarValue)).ast
      val astQuat = quatOf[Int]
      val (stablized, state) = StablizeLifts.stablize(ast)
      stablized must matchPattern {
        case ScalarValueLift("scalarValue", StablizeLifts.Token(0), _, `astQuat`) =>
      }
      state.replaceTable mustEqual (IMap(StablizeLifts.Token(0) -> scalarValue))
      StablizeLifts.revert(stablized, state) mustEqual (ast)
    }
    "ScalarQueryLift" in {
      val scalarQuery = Seq(1, 2, 3)
      val ast = quote(liftQuery(scalarQuery)).ast
      val astQuat = ast.quat
      val (stablized, state) = StablizeLifts.stablize(ast)
      stablized must matchPattern {
        case ScalarQueryLift("scalarQuery", StablizeLifts.Token(0), _, `astQuat`) =>
      }
      StablizeLifts.revert(stablized, state) mustEqual (ast)
    }
    "CaseClassValueLift" in {

      val caseClass = Foo(0L)
      val ast = quote(lift(caseClass)).ast
      val astQuat = quatOf[Foo]
      val (stablized, state) = StablizeLifts.stablize(ast)
      stablized must matchPattern {
        case CaseClassValueLift("caseClass", StablizeLifts.Token(0), `astQuat`) =>
      }
      state.replaceTable mustEqual (IMap(StablizeLifts.Token(0) -> caseClass))
      StablizeLifts.revert(stablized, state) mustEqual (ast)
    }

    "CaseClassQueryLift" in {
      val caseClasses = Seq(Foo(0L), Foo(1L))
      val ast = quote(liftQuery(caseClasses)).ast
      val astQuat = ast.quat
      val (stablized, state) = StablizeLifts.stablize(ast)
      stablized must matchPattern {
        case CaseClassQueryLift("caseClasses", StablizeLifts.Token(0), `astQuat`) =>
      }
      state.replaceTable mustEqual (IMap(StablizeLifts.Token(0) -> caseClasses))
      StablizeLifts.revert(stablized, state) mustEqual (ast)
    }

    "multiple lifts" in {
      val a = "s"
      val b = 2
      val ast = quote(lift(a) + lift(b)).ast
      val quatA = Quat.Value
      val quatB = Quat.Value
      val (stablized, state) = StablizeLifts.stablize(ast)
      stablized must matchPattern {
        case BinaryOperation(
          ScalarValueLift("a", StablizeLifts.Token(0), _, `quatA`),
          StringOperator.`+`, ScalarValueLift("b", StablizeLifts.Token(1), _, `quatB`)) =>
      }
      val expectedTable = IMap(StablizeLifts.Token(0) -> a, StablizeLifts.Token(1) -> b)
      state.replaceTable must contain theSameElementsAs (expectedTable)
      StablizeLifts.revert(stablized, state) mustEqual (ast)
    }
  }
}
