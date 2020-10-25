package io.getquill.ast

sealed trait JoinType

case object InnerJoin extends JoinType
case object LeftJoin extends JoinType
case object RightJoin extends JoinType
case object FullJoin extends JoinType
