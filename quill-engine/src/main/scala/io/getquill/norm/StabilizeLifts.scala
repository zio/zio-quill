package io.getquill.norm

import io.getquill.ast._
import scala.collection.immutable.{Map => IMap}

private[getquill] object StabilizeLifts {

  def stabilize(ast: Ast): (Ast, State) = {
    val (a, t) = StubLiftValues(State(IMap.empty, Token(0))).apply(ast)
    (a, t.state)
  }

  def revert(ast: Ast, state: State): Ast =
    RevertLiftValues(state).apply(ast)

  final case class State(
    replaceTable: IMap[Token, Any],
    nextToken: Token
  ) {
    def addReplace(t: Token, value: Any): State =
      this.copy(
        replaceTable = replaceTable + (t -> value),
        nextToken = Token(t.id + 1)
      )
  }

  final case class Token(id: Long)

  final case class RevertLiftValues(state: State) extends StatelessTransformer {
    override def apply(ast: Ast): Ast = ast match {
      case l: Lift => applyLift(l)
      case others  => super.apply(others)
    }

    def applyLift(ast: Lift): Lift = ast match {
      case l: ScalarValueLift =>
        val value = state.replaceTable(l.value.asInstanceOf[Token])
        l.copy(value = value)
      case l: ScalarQueryLift =>
        val value = state.replaceTable(l.value.asInstanceOf[Token])
        l.copy(value = value)
      case l: CaseClassValueLift =>
        val value = state.replaceTable(l.value.asInstanceOf[Token])
        l.copy(value = value)
      case l: CaseClassQueryLift =>
        val value = state.replaceTable(l.value.asInstanceOf[Token])
        l.copy(value = value)

    }
  }

  final case class StubLiftValues(state: State) extends StatefulTransformer[State] {
    override def apply(e: Ast): (Ast, StatefulTransformer[State]) = e match {
      case l: Lift =>
        val (ast, ss) = applyLift(l)
        (ast, StubLiftValues(ss))
      case others =>
        super.apply(others)
    }

    private def applyLift(ast: Lift): (Ast, State) = ast match {
      case l: ScalarValueLift =>
        val stub       = state.nextToken
        val stabilized = l.copy(value = stub)
        stabilized -> state.addReplace(stub, l.value)
      case l: ScalarQueryLift =>
        val stub       = state.nextToken
        val stabilized = l.copy(value = stub)
        stabilized -> state.addReplace(stub, l.value)
      case l: CaseClassValueLift =>
        val stub       = state.nextToken
        val stabilized = l.copy(value = stub)
        stabilized -> state.addReplace(stub, l.value)
      case l: CaseClassQueryLift =>
        val stub       = state.nextToken
        val stabilized = l.copy(value = stub)
        stabilized -> state.addReplace(stub, l.value)
    }
  }
}
