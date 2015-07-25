package io

import language.experimental.macros

package object getquill {

  def from[T]: Queryable[T] = ???
  def query[T](q: Queryable[T]): Queryable[T] = macro QueryableMacro.query[T]

  def partial[P1, T](f: P1 => T): Partial1[P1, T] = macro PartialMacro.create1[P1, T]
  def partial[P1, P2, T](f: (P1, P2) => T): Partial2[P1, P2, T] = macro PartialMacro.create2[P1, P2, T]
}