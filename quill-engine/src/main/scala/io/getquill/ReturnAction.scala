package io.getquill

sealed trait ReturnAction
object ReturnAction {
  case object ReturnNothing extends ReturnAction
  case class ReturnColumns(columns: List[String]) extends ReturnAction
  case object ReturnRecord extends ReturnAction
}
