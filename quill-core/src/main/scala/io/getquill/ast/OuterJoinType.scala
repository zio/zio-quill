package io.getquill.ast

sealed trait OuterJoinType

object LeftJoin extends OuterJoinType
object RightJoin extends OuterJoinType
object FullJoin extends OuterJoinType