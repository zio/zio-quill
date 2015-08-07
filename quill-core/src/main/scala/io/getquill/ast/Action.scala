package io.getquill.ast

sealed trait Action

case class Update(query: Query, assingments: List[Assignment]) extends Action
case class Insert(query: Query, assingments: List[Assignment]) extends Action
case class Delete(query: Query) extends Action

case class Assignment(property: Property, value: Expr)