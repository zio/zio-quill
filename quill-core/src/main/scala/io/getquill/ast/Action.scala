package io.getquill.ast

sealed trait Action

case class Update(query: Ast, assingments: List[Assignment]) extends Action
case class Insert(query: Ast, assingments: List[Assignment]) extends Action
case class Delete(query: Ast) extends Action

case class Assignment(property: Property, value: Ast)