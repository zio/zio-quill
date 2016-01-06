package io.getquill.ast

sealed trait OuterJoinType

case object LeftJoin extends OuterJoinType
case object RightJoin extends OuterJoinType
case object FullJoin extends OuterJoinType
