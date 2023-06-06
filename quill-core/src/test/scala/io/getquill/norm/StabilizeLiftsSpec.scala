package io.getquill.norm

import io.getquill.ast._
import io.getquill.base.Spec
import io.getquill.quat._
import io.getquill.MirrorContexts.testContext._
import scala.collection.immutable.{Map => IMap}

class StabilizeLiftsSpec extends Spec {

  case class Foo(id: Long)

  "stabilize lifts" - {
    "ScalarValueLift" in {
      val scalarValue         = 1
      val ast                 = quote(lift(scalarValue)).ast
      val astQuat             = quatOf[Int]
      val (stabilized, state) = StabilizeLifts.stabilize(ast)
      stabilized must matchPattern {
        case ScalarValueLift("scalarValue", External.Source.Parser, StabilizeLifts.Token(0), _, `astQuat`) =>
      }
      state.replaceTable mustEqual (IMap(StabilizeLifts.Token(0) -> scalarValue))
      StabilizeLifts.revert(stabilized, state) mustEqual (ast)
    }
    "ScalarQueryLift" in {
      val scalarQuery         = Seq(1, 2, 3)
      val ast                 = quote(liftQuery(scalarQuery)).ast
      val astQuat             = ast.quat
      val (stabilized, state) = StabilizeLifts.stabilize(ast)
      stabilized must matchPattern { case ScalarQueryLift("scalarQuery", StabilizeLifts.Token(0), _, `astQuat`) =>
      }
      StabilizeLifts.revert(stabilized, state) mustEqual (ast)
    }
    "CaseClassValueLift" in {

      val caseClass           = Foo(0L)
      val ast                 = quote(lift(caseClass)).ast
      val astQuat             = quatOf[Foo]
      val (stabilized, state) = StabilizeLifts.stabilize(ast)
      stabilized must matchPattern {
        case CaseClassValueLift("caseClass", "caseClass", StabilizeLifts.Token(0), `astQuat`) =>
      }
      state.replaceTable mustEqual (IMap(StabilizeLifts.Token(0) -> caseClass))
      StabilizeLifts.revert(stabilized, state) mustEqual (ast)
    }

    "CaseClassQueryLift" in {
      val caseClasses         = Seq(Foo(0L), Foo(1L))
      val ast                 = quote(liftQuery(caseClasses)).ast
      val astQuat             = ast.quat
      val (stabilized, state) = StabilizeLifts.stabilize(ast)
      stabilized must matchPattern { case CaseClassQueryLift("caseClasses", StabilizeLifts.Token(0), `astQuat`) =>
      }
      state.replaceTable mustEqual (IMap(StabilizeLifts.Token(0) -> caseClasses))
      StabilizeLifts.revert(stabilized, state) mustEqual (ast)
    }

    "multiple lifts" in {
      val a                   = "s"
      val b                   = 2
      val ast                 = quote(lift(a) + lift(b)).ast
      val quatA               = Quat.Value
      val quatB               = Quat.Value
      val (stabilized, state) = StabilizeLifts.stabilize(ast)
      stabilized must matchPattern {
        case BinaryOperation(
              ScalarValueLift("a", External.Source.Parser, StabilizeLifts.Token(0), _, `quatA`),
              StringOperator.`+`,
              ScalarValueLift("b", External.Source.Parser, StabilizeLifts.Token(1), _, `quatB`)
            ) =>
      }
      val expectedTable = IMap(StabilizeLifts.Token(0) -> a, StabilizeLifts.Token(1) -> b)
      state.replaceTable must contain theSameElementsAs (expectedTable)
      StabilizeLifts.revert(stabilized, state) mustEqual (ast)
    }
  }
}
