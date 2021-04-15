package io

package object getquill {
  type Query[+T] = Model.Query[T]
  type JoinQuery[A, B, R] = Model.JoinQuery[A, B, R]
  type EntityQueryModel[T] = Model.EntityQueryModel[T]
  type Action[E] = Model.Action[E]
  type Insert[E] = Model.Insert[E]
  type Update[E] = Model.Update[E]
  type ActionReturning[E, +Output] = Model.ActionReturning[E, Output]
  type Delete[E] = Model.Delete[E]
  type BatchAction[+A <: Model.QAC[_, _] with Action[_]] = Model.BatchAction[A]
}